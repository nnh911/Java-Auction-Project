package vn.uet.auction.network;

import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.service.*;
import vn.uet.auction.server.*;
import vn.uet.auction.protocol.*;
import vn.uet.auction.protocol.Request.*;
import vn.uet.auction.protocol.dto.*;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.model.item.ItemType;

/** Console integration client against real User/Item services; no Socket. */
public final class DispatcherSmokeTest {
    private static CommandDispatcher dispatcher;
    private static Response<?> send(Session session, Command command, Payload payload) {
        UUID id = UUID.randomUUID();
        Response<?> response = dispatcher.dispatch(session, new Request(id, command, payload));
        if (!id.equals(response.requestId())) throw new AssertionError("requestId");
        System.out.println(command + " -> " + (response.success() ? "OK" : response.errorCode()));
        return response;
    }
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static void error(Response<?> response, ErrorCode code) {
        check(!response.success() && response.errorCode() == code && response.data() == null);
    }
    public static void main(String[] args) {
        var state = new ApplicationState();
        var lock = new ReentrantLock();
        var users = new UserService(state, lock, new PasswordHasher(100000));
        dispatcher = new CommandDispatcher(users, new ItemService(state, lock), lock);
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
        send(seller, Command.LOGIN, new LoginPayload("seller_demo", "password"));
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
        System.out.println("DispatcherSmokeTest PASS");
    }
}
