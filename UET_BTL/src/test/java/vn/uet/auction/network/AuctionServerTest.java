package vn.uet.auction.network;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import vn.uet.auction.client.network.AuctionClient;
import vn.uet.auction.client.network.GatewayException;
import vn.uet.auction.model.auction.AuctionStatus;
import vn.uet.auction.model.item.ItemType;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.protocol.Command;
import vn.uet.auction.protocol.ErrorCode;
import vn.uet.auction.protocol.EventMessage;
import vn.uet.auction.protocol.Request;
import vn.uet.auction.protocol.Response;
import vn.uet.auction.protocol.dto.AuctionDetail;
import vn.uet.auction.protocol.dto.BidSummary;
import vn.uet.auction.protocol.dto.UserSummary;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.server.AuctionServer;
import vn.uet.auction.server.CommandDispatcher;
import vn.uet.auction.service.AuctionService;
import vn.uet.auction.service.ItemService;
import vn.uet.auction.service.UserService;

@Timeout(20)
class AuctionServerTest {
    @Test
    void twoSessionsCanRegisterListCreateAndBid() throws Exception {
        var state = new ApplicationState();
        var lock = new ReentrantLock();
        var users = new UserService(state, lock, new PasswordHasher(100_000));
        var items = new ItemService(state, lock);
        var auctions = new AuctionService(Clock.systemUTC(), lock, state, users, items);
        var admin = users.bootstrapAdmin("admin", "password");
        var executor = Executors.newSingleThreadExecutor();
        try (var server = new AuctionServer(loopback(0), 4,
                new CommandDispatcher(users, items, lock, auctions))) {
            var accepting = executor.submit(() -> {
                server.serve();
                return null;
            });
            try (var seller = new WireClient(server.localPort());
                 var bidder = new WireClient(server.localPort())) {
                assertEquals(ErrorCode.INVALID_REQUEST,
                        bidder.call(Command.LOGIN, new Request.EmptyPayload()).errorCode());
                registerAndLogin(seller, "seller", Role.SELLER);
                var bidderId = registerAndLogin(bidder, "bidder", Role.BIDDER);
                var itemId = (UUID) success(seller.call(Command.CREATE_ITEM,
                        new Request.CreateItemPayload(ItemType.ART, "Painting", "")));
                assertTrue(seller.call(Command.UPDATE_ITEM,
                        new Request.UpdateItemPayload(itemId, ItemType.ART, "Updated painting", "")).success());
                assertTrue(bidder.call(Command.GET_ITEM, new Request.ItemIdPayload(itemId)).success());
                var disposableItemId = (UUID) success(seller.call(Command.CREATE_ITEM,
                        new Request.CreateItemPayload(ItemType.ART, "Other painting", "")));
                assertTrue(seller.call(Command.DELETE_ITEM, new Request.ItemIdPayload(disposableItemId)).success());
                var now = Instant.now();
                var auction = (AuctionDetail) success(seller.call(Command.CREATE_AUCTION,
                        new Request.CreateAuctionPayload(itemId, BigDecimal.TEN,
                                now.minusSeconds(5), now.plusSeconds(600))));
                assertTrue(bidder.call(Command.LIST_ITEMS, new Request.EmptyPayload()).success());
                assertTrue(bidder.call(Command.LIST_AUCTIONS, new Request.EmptyPayload()).success());
                assertEquals(ErrorCode.FORBIDDEN, seller.call(Command.PLACE_BID,
                        new Request.PlaceBidPayload(auction.id(), new BigDecimal("12"))).errorCode());
                var placed = (AuctionDetail) success(bidder.call(Command.PLACE_BID,
                        new Request.PlaceBidPayload(auction.id(), new BigDecimal("12"))));
                assertEquals(new BigDecimal("12"), placed.currentPrice());
                assertEquals(ErrorCode.AUTHENTICATION_FAILED, seller.call(Command.LOGIN,
                        new Request.LoginPayload("seller", "wrong")).errorCode());
                assertEquals(ErrorCode.UNAUTHENTICATED,
                        seller.call(Command.LIST_ITEMS, new Request.EmptyPayload()).errorCode());
                users.setUserActive(admin.id(), bidderId, false);
                assertEquals(ErrorCode.FORBIDDEN,
                        bidder.call(Command.LIST_AUCTIONS, new Request.EmptyPayload()).errorCode());
            }
            try (var oversized = new WireClient(server.localPort())) {
                assertThrows(IOException.class, () -> oversized.call(Command.LOGIN,
                        new Request.LoginPayload("x".repeat(70_000), "password")));
            }
            try (var fresh = new AuctionClient(loopback(server.localPort()), 3_000, 3_000)) {
                var failure = assertThrows(ExecutionException.class,
                        () -> fresh.getAuction(UUID.randomUUID()).toCompletableFuture().get(5, TimeUnit.SECONDS));
                assertEquals(ErrorCode.UNAUTHENTICATED,
                        assertInstanceOf(GatewayException.class, failure.getCause()).getErrorCode());
            }
            server.close();
            accepting.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void sdkMatchesOutOfOrderResponsesAndReceivesInterleavedEvent() throws Exception {
        var firstId = UUID.randomUUID();
        var secondId = UUID.randomUUID();
        var eventReceived = new CountDownLatch(1);
        var now = Instant.now();
        var bid = new BidSummary(UUID.randomUUID(), secondId, UUID.randomUUID(), BigDecimal.TEN, now);
        var detail = new AuctionDetail(secondId, UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.ONE, BigDecimal.TEN, AuctionStatus.RUNNING,
                now.minusSeconds(60), now.plusSeconds(60), null, List.of(bid));
        var executor = Executors.newSingleThreadExecutor();
        try (var listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            var peer = executor.submit(() -> {
                try (var socket = listener.accept();
                     var output = new ObjectOutputStream(socket.getOutputStream())) {
                    output.flush();
                    try (var input = new ObjectInputStream(socket.getInputStream())) {
                        var first = (Request) input.readObject();
                        var second = (Request) input.readObject();
                        output.writeObject(new EventMessage(EventMessage.EventType.BID_ACCEPTED, firstId, Instant.now()));
                        output.writeObject(Response.ok(second.requestId(), detail));
                        output.writeObject(Response.fail(first.requestId(), ErrorCode.FORBIDDEN, "FORBIDDEN"));
                        output.flush();
                    }
                }
                return null;
            });
            try (var client = new AuctionClient(loopback(listener.getLocalPort()), 3_000, 3_000)) {
                client.addEventListener(event -> eventReceived.countDown());
                var first = client.getAuction(firstId).toCompletableFuture();
                var second = client.getAuction(secondId).toCompletableFuture();
                assertGatewayFailure(first, ErrorCode.FORBIDDEN);
                assertEquals(detail, second.get(5, TimeUnit.SECONDS));
                assertTrue(eventReceived.await(5, TimeUnit.SECONDS));
            }
            peer.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void sdkTimesOutWhenPeerDoesNotReply() throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try (var listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            var peer = executor.submit(() -> {
                try (var socket = listener.accept();
                     var output = new ObjectOutputStream(socket.getOutputStream())) {
                    output.flush();
                    try (var input = new ObjectInputStream(socket.getInputStream())) {
                        input.readObject();
                        input.readObject(); // Wait for disconnect without sending a response.
                    }
                } catch (IOException disconnected) {
                    // The client can close before all request bytes arrive.
                }
                return null;
            });
            try (var client = new AuctionClient(loopback(listener.getLocalPort()), 3_000, 250)) {
                var failure = assertThrows(ExecutionException.class,
                        () -> client.getAuction(UUID.randomUUID()).toCompletableFuture().get(5, TimeUnit.SECONDS));
                assertInstanceOf(TimeoutException.class, failure.getCause());
            }
            peer.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }
    }

    private static void assertGatewayFailure(java.util.concurrent.Future<AuctionDetail> result, ErrorCode code) {
        var failure = assertThrows(ExecutionException.class, () -> result.get(5, TimeUnit.SECONDS));
        assertEquals(code, assertInstanceOf(GatewayException.class, failure.getCause()).getErrorCode());
    }

    private static UUID registerAndLogin(WireClient client, String name, Role role) throws Exception {
        success(client.call(Command.REGISTER, new Request.RegisterPayload(name, "password", role)));
        return ((UserSummary) success(client.call(Command.LOGIN, new Request.LoginPayload(name, "password")))).id();
    }

    private static Serializable success(Response<?> response) {
        assertTrue(response.success(), response::message);
        return response.data();
    }

    private static InetSocketAddress loopback(int port) {
        return new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
    }

    private static final class WireClient implements AutoCloseable {
        private final Socket socket = new Socket();
        private final ObjectOutputStream output;
        private final ObjectInputStream input;

        WireClient(int port) throws IOException {
            try {
                socket.connect(loopback(port), 3_000);
                socket.setSoTimeout(5_000);
                output = new ObjectOutputStream(socket.getOutputStream());
                output.flush();
                input = new ObjectInputStream(socket.getInputStream());
            } catch (IOException failure) {
                socket.close();
                throw failure;
            }
        }

        Response<?> call(Command command, Request.Payload payload) throws Exception {
            var id = UUID.randomUUID();
            output.reset();
            output.writeObject(new Request(id, command, payload));
            output.flush();
            var response = assertInstanceOf(Response.class, input.readObject());
            assertEquals(id, response.requestId());
            return response;
        }

        @Override
        public void close() throws IOException {
            socket.close();
        }
    }
}
