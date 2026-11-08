package vn.uet.auction.auction;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;
import vn.uet.auction.model.item.ItemType;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.observer.AuctionEvent;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.service.AuctionManager;
import vn.uet.auction.service.AuctionService;
import vn.uet.auction.service.ItemService;
import vn.uet.auction.service.UserService;

class AuctionAuthorizationTest {
    private static final Instant START = Instant.parse("2026-11-08T01:00:00Z");
    private static final Instant END = START.plusSeconds(60);
    private static final String PASSWORD = "test-password";

    @Test
    void managerCreationPreventsItemEditsAndDeletion() {
        AuctionManager manager = AuctionManager.getInstance();
        UUID seller = manager.getUserService().register(
                "seller_" + UUID.randomUUID().toString().substring(0, 8), PASSWORD, Role.SELLER).id();
        ItemService items = manager.getItemService();
        UUID item = items.createItem(seller, ItemType.ART, "Painting", "");
        Instant now = Instant.now();
        var created = manager.createAuction(seller, item, BigDecimal.ONE, now, now.plusSeconds(600));

        assertEquals(item, manager.getAuction(created.id()).orElseThrow().itemId());
        assertThrows(IllegalStateException.class,
                () -> items.updateItem(seller, item, ItemType.ART, "Updated", ""));
        assertThrows(IllegalStateException.class, () -> items.deleteItem(seller, item));
    }

    @Test
    void otherSellerAndInactiveOwnerCannotCreateAuction() {
        Fixture fixture = new Fixture();
        UUID other = fixture.users.register("other_seller", PASSWORD, Role.SELLER).id();
        UUID admin = fixture.users.bootstrapAdmin("admin", PASSWORD).id();

        assertThrows(SecurityException.class, () -> fixture.create(other));
        fixture.users.setUserActive(admin, fixture.owner, false);
        assertThrows(SecurityException.class, () -> fixture.create(fixture.owner));
        assertTrue(fixture.auctions.listAuctions().isEmpty());
    }

    @Test
    void sellerAndLockedBidderCannotChangeAuctionOrEmitAcceptedBid() {
        Fixture fixture = new Fixture();
        UUID bidder = fixture.users.register("bidder", PASSWORD, Role.BIDDER).id();
        UUID seller = fixture.users.register("other_seller", PASSWORD, Role.SELLER).id();
        UUID admin = fixture.users.bootstrapAdmin("admin", PASSWORD).id();
        var created = fixture.create(fixture.owner);
        var events = new ArrayList<AuctionEvent>();
        fixture.auctions.addListener(event -> {
            assertFalse(fixture.lock.isHeldByCurrentThread());
            events.add(event);
        });

        assertThrows(SecurityException.class,
                () -> fixture.auctions.placeBid(created.id(), seller, BigDecimal.TEN));
        assertEquals(created, fixture.auctions.getAuction(created.id()).orElseThrow());
        assertTrue(events.isEmpty());

        var accepted = fixture.auctions.placeBid(created.id(), bidder, BigDecimal.TEN);
        assertEquals(BigDecimal.TEN, accepted.currentPrice());
        assertEquals(1, accepted.bidCount());
        assertEquals(AuctionEvent.Type.BID_ACCEPTED, assertSingleEvent(events).type());

        fixture.users.setUserActive(admin, bidder, false);
        for (Instant time : new Instant[] {START, END}) {
            fixture.clock.set(time);
            assertThrows(SecurityException.class,
                    () -> fixture.auctions.placeBid(created.id(), bidder, BigDecimal.valueOf(20)));
            assertEquals(accepted, fixture.auctions.getAuction(created.id()).orElseThrow());
            assertEquals(1, events.size());
        }
    }

    @Test
    void activeAdminCancelsAnotherSellersAuctionButCannotCancelPaidAuction() {
        Fixture fixture = new Fixture();
        UUID admin = fixture.users.bootstrapAdmin("admin", PASSWORD).id();
        UUID bidder = fixture.users.register("bidder", PASSWORD, Role.BIDDER).id();
        UUID otherSeller = fixture.users.register("other_seller", PASSWORD, Role.SELLER).id();
        var first = fixture.create(fixture.owner);
        var events = new ArrayList<AuctionEvent>();
        fixture.auctions.addListener(events::add);
        assertThrows(SecurityException.class, () -> fixture.auctions.cancelAuction(otherSeller, first.id()));
        fixture.users.setUserActive(admin, fixture.owner, false);
        assertThrows(SecurityException.class, () -> fixture.auctions.cancelAuction(fixture.owner, first.id()));
        assertEquals(first, fixture.auctions.getAuction(first.id()).orElseThrow());
        assertTrue(events.isEmpty());
        assertEquals(vn.uet.auction.model.auction.AuctionStatus.CANCELED,
                fixture.auctions.cancelAuction(admin, first.id()).status());
        assertEquals(AuctionEvent.Type.AUCTION_CANCELED, assertSingleEvent(events).type());

        fixture.users.setUserActive(admin, fixture.owner, true);
        var second = fixture.create(fixture.owner);
        fixture.auctions.placeBid(second.id(), bidder, BigDecimal.TEN);
        fixture.clock.set(END);
        var paid = fixture.auctions.confirmPayment(bidder, second.id());
        int eventCount = events.size();
        assertThrows(IllegalStateException.class, () -> fixture.auctions.cancelAuction(admin, second.id()));
        assertEquals(paid, fixture.auctions.getAuction(second.id()).orElseThrow());
        assertEquals(eventCount, events.size());
    }

    private static AuctionEvent assertSingleEvent(ArrayList<AuctionEvent> events) {
        assertEquals(1, events.size());
        return events.get(0);
    }

    private static final class Fixture {
        private final MutableClock clock = new MutableClock(START);
        private final ApplicationState state = new ApplicationState();
        private final ReentrantLock lock = new ReentrantLock();
        private final UserService users = new UserService(state, lock, new PasswordHasher());
        private final ItemService items = new ItemService(state, lock);
        private final AuctionService auctions = new AuctionService(clock, lock, state, users, items);
        private final UUID owner = users.register("seller", PASSWORD, Role.SELLER).id();
        private final UUID item = items.createItem(owner, ItemType.ART, "Painting", "");

        private vn.uet.auction.service.AuctionSnapshot create(UUID seller) {
            return auctions.createAuction(seller, item, BigDecimal.ONE, START, END);
        }
    }
}
