package vn.uet.auction.server;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import vn.uet.auction.exception.AuctionClosedException;
import vn.uet.auction.exception.AuthenticationException;
import vn.uet.auction.protocol.*;
import vn.uet.auction.protocol.Request.*;
import vn.uet.auction.protocol.dto.*;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.protocol.mapper.ProtocolMapper;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.service.AuctionService;
import vn.uet.auction.service.StateTransactions;
import vn.uet.auction.service.ItemService;
import vn.uet.auction.service.UserService;

/** Routes protocol commands to services sharing the same application state and lock. */
public final class CommandDispatcher {
    private final UserService users;
    private final ItemService items;
    private final ReentrantLock lock;
    private final AuctionService auctions;
    private final StateTransactions transactions;
    private final PasswordHasher hasher;

    public CommandDispatcher(UserService users, ItemService items, ReentrantLock lock) {
        this(users, items, lock, null);
    }

    public CommandDispatcher(UserService users, ItemService items, ReentrantLock lock,
                             AuctionService auctions) {
        this(users, items, lock, auctions, null, null);
    }

    CommandDispatcher(UserService users, ItemService items, ReentrantLock lock,
                      AuctionService auctions, StateTransactions transactions, PasswordHasher hasher) {
        this.transactions = transactions;
        this.hasher = hasher;
        this.auctions = auctions;
        this.users = Objects.requireNonNull(users);
        this.items = Objects.requireNonNull(items);
        this.lock = Objects.requireNonNull(lock);
    }

    /** Request and session must be non-null; response always retains requestId. */
    public Response<? extends Serializable> dispatch(Session session, Request request) {
        Objects.requireNonNull(session);
        Objects.requireNonNull(request);
        switch (request.command()) {
            case CREATE_AUCTION, LIST_AUCTIONS, GET_AUCTION, PLACE_BID, CONFIRM_PAYMENT, CANCEL_AUCTION:
                return validPayload(request)
                        ? dispatchAuction(session, request)
                        : fail(request, ErrorCode.INVALID_REQUEST);
            default:
                break;
        }
        if (transactions != null && writesUsersOrItems(request.command())) {
            try {
                return transactions.write(candidate -> {
                    var candidateUsers = new UserService(candidate, lock, hasher);
                    var candidateItems = new ItemService(candidate, lock);
                    return dispatchUsersAndItems(session, request, candidateUsers, candidateItems);
                }, Response::success);
            } catch (IOException failure) {
                return fail(request, ErrorCode.INTERNAL_ERROR);
            }
        }
        return dispatchUsersAndItems(session, request, users, items);
    }

    private Response<? extends Serializable> dispatchUsersAndItems(
            Session session, Request request, UserService users, ItemService items) {
        lock.lock();
        try {
            if (request.command() == Command.LOGIN) session.clear();
            if (!validPayload(request)) return fail(request, ErrorCode.INVALID_REQUEST);
            if (request.command() == Command.LOGIN) {
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
        } catch (AuthenticationException e) {
            return fail(request, ErrorCode.AUTHENTICATION_FAILED);
        } catch (SecurityException e) {
            return fail(request, ErrorCode.FORBIDDEN);
        } catch (IllegalArgumentException e) {
            return fail(request, ErrorCode.INVALID_REQUEST);
        } catch (IllegalStateException e) {
            return fail(request, ErrorCode.CONFLICT);
        } catch (RuntimeException e) {
            return fail(request, ErrorCode.INTERNAL_ERROR);
        } finally { lock.unlock(); }
    }

    private Response<? extends Serializable> dispatchAuction(Session session, Request request) {
        if (!session.isAuthenticated()) return fail(request, ErrorCode.UNAUTHENTICATED);
        UUID actor = session.userId();
        try {
            users.requireActive(actor);
            if (auctions == null) return fail(request, ErrorCode.UNSUPPORTED_COMMAND);

            // AuctionService owns the mutation lock and publishes events after releasing it.
            Serializable result;
            switch (request.command()) {
                case CREATE_AUCTION -> {
                    CreateAuctionPayload p = (CreateAuctionPayload) request.payload();
                    result = ProtocolMapper.toAuctionDetail(auctions.createAuction(
                            actor, p.itemId(), p.startingPrice(), p.startTime(), p.endTime()));
                }
                case LIST_AUCTIONS -> {
                    var summaries = new ArrayList<AuctionSummary>();
                    auctions.listAuctions().forEach(a -> summaries.add(ProtocolMapper.toAuctionSummary(a)));
                    result = summaries;
                }
                case GET_AUCTION -> {
                    var auction = auctions.getAuction(((AuctionIdPayload) request.payload()).auctionId());
                    if (auction.isEmpty()) return fail(request, ErrorCode.NOT_FOUND);
                    result = ProtocolMapper.toAuctionDetail(auction.get());
                }
                case PLACE_BID -> {
                    PlaceBidPayload p = (PlaceBidPayload) request.payload();
                    result = ProtocolMapper.toAuctionDetail(auctions.placeBid(p.auctionId(), actor, p.amount()));
                }
                case CONFIRM_PAYMENT -> result = ProtocolMapper.toAuctionDetail(
                        auctions.confirmPayment(actor, ((AuctionIdPayload) request.payload()).auctionId()));
                case CANCEL_AUCTION -> result = ProtocolMapper.toAuctionDetail(
                        auctions.cancelAuction(actor, ((AuctionIdPayload) request.payload()).auctionId()));
                default -> { return fail(request, ErrorCode.UNSUPPORTED_COMMAND); }
            }
            return Response.ok(request.requestId(), result);
        } catch (AuctionService.Rejection e) {
            return fail(request, switch (e.reason()) {
                case NOT_FOUND -> ErrorCode.NOT_FOUND;
                case FORBIDDEN -> ErrorCode.FORBIDDEN;
                case INVALID_BID -> ErrorCode.INVALID_BID;
                case CONFLICT -> ErrorCode.CONFLICT;
            });
        } catch (SecurityException e) {
            return fail(request, ErrorCode.FORBIDDEN);
        } catch (IllegalArgumentException e) {
            return fail(request, ErrorCode.INVALID_REQUEST);
        } catch (AuctionClosedException e) {
            return fail(request, ErrorCode.AUCTION_CLOSED);
        } catch (IllegalStateException e) {
            return fail(request, ErrorCode.CONFLICT);
        } catch (RuntimeException e) {
            return fail(request, ErrorCode.INTERNAL_ERROR);
        }
    }

    private static boolean writesUsersOrItems(Command command) {
        return switch (command) {
            case REGISTER, SET_USER_ACTIVE, CREATE_ITEM, UPDATE_ITEM, DELETE_ITEM -> true;
            default -> false;
        };
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
