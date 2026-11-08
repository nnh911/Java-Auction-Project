package vn.uet.auction.network;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import vn.uet.auction.auction.MutableClock;
import vn.uet.auction.model.auction.AuctionStatus;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.service.*;
import vn.uet.auction.server.*;
import vn.uet.auction.protocol.*;
import vn.uet.auction.protocol.Request.*;
import vn.uet.auction.protocol.dto.*;
import vn.uet.auction.protocol.mapper.ProtocolMapper;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.model.item.ItemType;

/** Console integration client against real User/Item/Auction services; no Socket. */
public final class DispatcherSmokeTest {
    private CommandDispatcher dispatcher;
    private Response<?> send(Session session, Command command, Payload payload) {
        UUID id = UUID.randomUUID();
        Response<?> response = dispatcher.dispatch(session, new Request(id, command, payload));
        assertEquals(id, response.requestId());
        System.out.println(command + " -> " + (response.success() ? "OK" : response.errorCode()));
        return response;
    }
    private static void check(boolean value) { assertTrue(value); }
    private static void error(Response<?> response, ErrorCode code) {
        assertFalse(response.success());
        assertEquals(code, response.errorCode());
        assertEquals(code.name(), response.message());
        assertNull(response.data());
    }
    public static void main(String[] args) {
        new DispatcherSmokeTest().dispatchesAllCommandsWithRealServices();
    }

    @Test
    void dispatchesAllCommandsWithRealServices() {
        var state = new ApplicationState();
        var lock = new ReentrantLock();
        var users = new UserService(state, lock, new PasswordHasher(100000));
        var items = new ItemService(state, lock);
        Instant now = Instant.parse("2026-11-08T03:00:00Z");
        var clock = new MutableClock(now);
        var auctions = new AuctionService(clock, lock, state, users, items);
        dispatcher = new CommandDispatcher(users, items, lock, auctions);
        for (Command command : Command.values()) {
            Payload wrong = switch (command) {
                case LIST_USERS, LIST_ITEMS, LIST_AUCTIONS -> new ItemIdPayload(UUID.randomUUID());
                default -> new EmptyPayload();
            };
            var request = new Request(UUID.randomUUID(), command, wrong);
            var invalid = dispatcher.dispatch(new Session(), request);
            error(invalid, ErrorCode.INVALID_REQUEST);
            assertEquals(request.requestId(), invalid.requestId());
            assertEquals(invalid, dispatcher.dispatch(new Session(), request));
        }
        var seller = new Session();
        var other = new Session();
        var admin = new Session();
        users.bootstrapAdmin("admin_demo", "password");
        error(send(seller, Command.LIST_ITEMS, new EmptyPayload()), ErrorCode.UNAUTHENTICATED);
        error(send(seller, Command.REGISTER, new RegisterPayload("bad_admin", "password", Role.ADMIN)), ErrorCode.FORBIDDEN);
        var registered = send(seller, Command.REGISTER, new RegisterPayload("seller_demo", "password", Role.SELLER));
        check(registered.success());
        UUID sellerId = ((UserSummary) registered.data()).id();
        error(send(seller, Command.REGISTER, new RegisterPayload("seller_demo", "password", Role.SELLER)), ErrorCode.CONFLICT);
        check(send(seller, Command.LOGIN, new LoginPayload("seller_demo", "password")).success());
        error(send(seller, Command.LOGIN, new LoginPayload("seller_demo", "wrong")), ErrorCode.AUTHENTICATION_FAILED);
        check(!seller.isAuthenticated());
        check(send(seller, Command.LOGIN, new LoginPayload("seller_demo", "password")).success());
        error(send(seller, Command.LOGIN, new EmptyPayload()), ErrorCode.INVALID_REQUEST);
        error(send(seller, Command.LIST_ITEMS, new EmptyPayload()), ErrorCode.UNAUTHENTICATED);
        check(send(seller, Command.LOGIN, new LoginPayload("seller_demo", "password")).success());
        error(send(seller, Command.GET_ITEM, new EmptyPayload()), ErrorCode.INVALID_REQUEST);
        UUID item = (UUID) send(seller, Command.CREATE_ITEM,
                new CreateItemPayload(ItemType.ART, "Art", "Description")).data();
        check(send(seller, Command.LIST_ITEMS, new EmptyPayload()).success());
        check(send(seller, Command.GET_ITEM, new ItemIdPayload(item)).data() instanceof ItemDetail);
        check(send(seller, Command.UPDATE_ITEM, new UpdateItemPayload(item, ItemType.ART, "Updated", "")).data() instanceof Ack);
        send(other, Command.REGISTER, new RegisterPayload("other_seller", "password", Role.SELLER));
        send(other, Command.LOGIN, new LoginPayload("other_seller", "password"));
        error(send(other, Command.DELETE_ITEM, new ItemIdPayload(item)), ErrorCode.FORBIDDEN);
        check(send(seller, Command.DELETE_ITEM, new ItemIdPayload(item)).success());
        error(send(seller, Command.DELETE_ITEM, new ItemIdPayload(item)), ErrorCode.NOT_FOUND);
        send(admin, Command.LOGIN, new LoginPayload("admin_demo", "password"));
        check(send(admin, Command.LIST_USERS, new EmptyPayload()).data() instanceof ListResult<?>);
        check(send(admin, Command.SET_USER_ACTIVE, new SetUserActivePayload(sellerId, false)).success());
        error(send(seller, Command.LIST_ITEMS, new EmptyPayload()), ErrorCode.FORBIDDEN);
        check(state.getItems().isEmpty());
        check(send(admin, Command.SET_USER_ACTIVE, new SetUserActivePayload(sellerId, true)).success());
        verifyAuctionFlow(seller, other, admin, auctions, lock, clock);
        System.out.println("DispatcherSmokeTest PASS");
    }

    private void verifyAuctionFlow(Session seller, Session other, Session admin,
                                          AuctionService auctions, ReentrantLock lock, MutableClock clock) {
        Instant now = clock.instant();
        var events = new ArrayList<EventMessage>();
        auctions.addListener(event -> {
            check(!lock.isHeldByCurrentThread());
            EventMessage message = ProtocolMapper.toEventMessage(event);
            events.add(message);
            System.out.println("Observer -> " + message.eventType());
        });

        UUID item = (UUID) send(seller, Command.CREATE_ITEM,
                new CreateItemPayload(ItemType.ART, "Auction item", "")).data();
        var create = new CreateAuctionPayload(item, BigDecimal.TEN, now, now.plusSeconds(60));
        error(send(other, Command.CREATE_AUCTION, create), ErrorCode.FORBIDDEN);
        var auction = (AuctionDetail) send(seller, Command.CREATE_AUCTION, create).data();
        var auctionId = new AuctionIdPayload(auction.id());
        error(send(seller, Command.DELETE_ITEM, new ItemIdPayload(item)), ErrorCode.CONFLICT);
        check(send(seller, Command.LIST_AUCTIONS, new EmptyPayload()).data() instanceof ArrayList<?>);

        var bidder = new Session();
        var registered = (UserSummary) send(bidder, Command.REGISTER,
                new RegisterPayload("bidder_demo", "password", Role.BIDDER)).data();
        check(send(bidder, Command.LOGIN, new LoginPayload("bidder_demo", "password")).success());
        error(send(bidder, Command.PLACE_BID, new PlaceBidPayload(UUID.randomUUID(), BigDecimal.TEN)),
                ErrorCode.NOT_FOUND);
        error(send(bidder, Command.PLACE_BID, new PlaceBidPayload(auction.id(), BigDecimal.TEN)),
                ErrorCode.INVALID_BID);
        int eventsBeforeBid = events.size();
        var bid = new PlaceBidPayload(auction.id(), BigDecimal.valueOf(20));
        error(send(seller, Command.PLACE_BID, bid), ErrorCode.FORBIDDEN);
        check(send(admin, Command.SET_USER_ACTIVE, new SetUserActivePayload(registered.id(), false)).success());
        error(send(bidder, Command.PLACE_BID, bid), ErrorCode.FORBIDDEN);
        check(send(admin, Command.SET_USER_ACTIVE, new SetUserActivePayload(registered.id(), true)).success());
        var updated = (AuctionDetail) send(bidder, Command.PLACE_BID, bid).data();
        check(updated.history().size() == 1 && updated.history().get(0).bidderId().equals(registered.id()));
        check(updated.equals(send(bidder, Command.GET_AUCTION, auctionId).data()));
        check(events.size() == eventsBeforeBid + 1);
        check(events.get(eventsBeforeBid).equals(
                new EventMessage(EventMessage.EventType.BID_ACCEPTED, auction.id(), now)));
        error(send(bidder, Command.CONFIRM_PAYMENT, auctionId), ErrorCode.CONFLICT);
        error(send(other, Command.CANCEL_AUCTION, auctionId), ErrorCode.FORBIDDEN);
        check(send(seller, Command.CANCEL_AUCTION, auctionId).success());
        check(events.get(events.size() - 1).eventType() == EventMessage.EventType.AUCTION_CANCELED);
        error(send(bidder, Command.PLACE_BID, bid), ErrorCode.AUCTION_CLOSED);
        assertEquals(1, ((AuctionDetail) send(bidder, Command.GET_AUCTION, auctionId).data())
                .history().size());

        var payable = (AuctionDetail) send(seller, Command.CREATE_AUCTION, create).data();
        check(send(bidder, Command.PLACE_BID,
                new PlaceBidPayload(payable.id(), bid.amount())).success());
        clock.set(create.endTime());
        var paid = (AuctionDetail) send(bidder, Command.CONFIRM_PAYMENT,
                new AuctionIdPayload(payable.id())).data();
        assertEquals(AuctionStatus.PAID, paid.status());
        assertEquals(registered.id(), paid.winnerId());
        assertEquals(EventMessage.EventType.PAYMENT_CONFIRMED, events.get(events.size() - 1).eventType());
    }
}
