package vn.uet.auction.server;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import vn.uet.auction.protocol.*;
import vn.uet.auction.protocol.Request.*;
import vn.uet.auction.protocol.dto.*;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.service.ItemService;
import vn.uet.auction.service.UserService;

/** Week 7 User/Item dispatcher. Supply the same application lock as both services. */
public final class CommandDispatcher {
    private final UserService users;
    private final ItemService items;
    private final ReentrantLock lock;

    public CommandDispatcher(UserService users, ItemService items, ReentrantLock lock) {
        this.users = Objects.requireNonNull(users);
        this.items = Objects.requireNonNull(items);
        this.lock = Objects.requireNonNull(lock);
    }

    /** Request and session must be non-null; response always retains requestId. */
    public Response<? extends Serializable> dispatch(Session session, Request request) {
        Objects.requireNonNull(session);
        Objects.requireNonNull(request);
        lock.lock();
        try {
            if (!validPayload(request)) return fail(request, ErrorCode.INVALID_REQUEST);
            if (request.command() == Command.LOGIN) {
                session.clear();
                LoginPayload p = (LoginPayload) request.payload();
                UserSummary user = users.login(p.username(), p.password());
                session.bind(user);
                return Response.ok(request.requestId(), user);
            }
            if (request.command() == Command.REGISTER) {
                RegisterPayload p = (RegisterPayload) request.payload();
                return Response.ok(request.requestId(), users.register(p.username(), p.password(), p.role()));
            }
            if (!session.isAuthenticated()) return fail(request, ErrorCode.UNAUTHENTICATED);
            UUID actor = session.userId();
            users.requireActive(actor);
            Serializable result;
            switch (request.command()) {
                case LIST_USERS -> result = new ListResult<>(users.listUsers(actor));
                case SET_USER_ACTIVE -> {
                    SetUserActivePayload p = (SetUserActivePayload) request.payload();
                    users.requireRole(actor, Role.ADMIN);
                    if (users.listUsers(actor).stream().noneMatch(u -> u.id().equals(p.userId())))
                        return fail(request, ErrorCode.NOT_FOUND);
                    users.setUserActive(actor, p.userId(), p.active());
                    result = new Ack();
                }
                case CREATE_ITEM -> {
                    CreateItemPayload p = (CreateItemPayload) request.payload();
                    result = items.createItem(actor, p.type(), p.name(), p.description());
                }
                case LIST_ITEMS -> result = new ListResult<>(items.listItems(actor));
                case GET_ITEM -> {
                    var detail = items.getItem(actor, ((ItemIdPayload) request.payload()).itemId());
                    if (detail.isEmpty()) return fail(request, ErrorCode.NOT_FOUND);
                    result = detail.get();
                }
                case UPDATE_ITEM, DELETE_ITEM -> {
                    users.requireRole(actor, Role.SELLER);
                    UUID id = request.payload() instanceof UpdateItemPayload p
                            ? p.itemId() : ((ItemIdPayload) request.payload()).itemId();
                    if (items.getItem(actor, id).isEmpty()) return fail(request, ErrorCode.NOT_FOUND);
                    if (request.payload() instanceof UpdateItemPayload p)
                        items.updateItem(actor, id, p.type(), p.name(), p.description());
                    else items.deleteItem(actor, id);
                    result = new Ack();
                }
                default -> { return fail(request, ErrorCode.UNSUPPORTED_COMMAND); }
            }
            return Response.ok(request.requestId(), result);
        } catch (SecurityException e) {
            return fail(request, request.command() == Command.LOGIN
                    ? ErrorCode.AUTHENTICATION_FAILED : ErrorCode.FORBIDDEN);
        } catch (IllegalArgumentException | NullPointerException e) {
            return fail(request, ErrorCode.INVALID_REQUEST);
        } catch (IllegalStateException e) {
            return fail(request, ErrorCode.CONFLICT);
        } catch (RuntimeException e) {
            return fail(request, ErrorCode.INTERNAL_ERROR);
        } finally { lock.unlock(); }
    }

    private static boolean validPayload(Request request) {
        return switch (request.command()) {
            case LOGIN -> request.payload() instanceof LoginPayload;
            case REGISTER -> request.payload() instanceof RegisterPayload;
            case LIST_USERS, LIST_ITEMS -> request.payload() instanceof EmptyPayload;
            case SET_USER_ACTIVE -> request.payload() instanceof SetUserActivePayload;
            case CREATE_ITEM -> request.payload() instanceof CreateItemPayload;
            case GET_ITEM, DELETE_ITEM -> request.payload() instanceof ItemIdPayload;
            case UPDATE_ITEM -> request.payload() instanceof UpdateItemPayload;
            case CREATE_AUCTION -> request.payload() instanceof CreateAuctionPayload;
            case PLACE_BID -> request.payload() instanceof PlaceBidPayload;
            case GET_AUCTION, CONFIRM_PAYMENT, CANCEL_AUCTION -> request.payload() instanceof AuctionIdPayload;
            case LIST_AUCTIONS -> request.payload() instanceof EmptyPayload;
        };
    }

    private static Response<Serializable> fail(Request request, ErrorCode code) {
        return Response.fail(request.requestId(), code, code.name());
    }
}
