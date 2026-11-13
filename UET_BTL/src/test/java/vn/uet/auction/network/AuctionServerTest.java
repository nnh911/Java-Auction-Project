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
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
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
import org.junit.jupiter.api.io.TempDir;
import vn.uet.auction.client.network.AuctionClient;
import vn.uet.auction.client.network.GatewayException;
import vn.uet.auction.model.auction.AuctionStatus;
import vn.uet.auction.model.item.ItemType;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.persistence.FileStateStore;
import vn.uet.auction.persistence.StateStore;
import vn.uet.auction.protocol.Command;
import vn.uet.auction.protocol.ErrorCode;
import vn.uet.auction.protocol.EventMessage;
import vn.uet.auction.protocol.Request;
import vn.uet.auction.protocol.Response;
import vn.uet.auction.protocol.dto.AuctionDetail;
import vn.uet.auction.protocol.dto.BidSummary;
import vn.uet.auction.protocol.dto.ItemDetail;
import vn.uet.auction.protocol.dto.UserSummary;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.server.AuctionServer;
import vn.uet.auction.server.CommandDispatcher;
import vn.uet.auction.server.PersistentServerFixture;
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
    void readsCommittedBidThroughSocketAfterRestart(@TempDir Path directory) throws Exception {
        var store = new FileStateStore(directory.resolve("auction-state.bin"));
        var now = Instant.parse("2026-11-13T00:00:00Z");
        var time = new AtomicReference<>(now);
        var clock = new Clock() {
            @Override
            public ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return Clock.fixed(instant(), zone);
            }

            @Override
            public Instant instant() {
                return time.get();
            }
        };
        var executor = Executors.newSingleThreadExecutor();
        try {
            AuctionDetail committed;
            UUID bidderId;
            UUID savedItemId;
            UUID deletedItemId;
            try (var fixture = PersistentServerFixture.open(loopback(0), store, clock)) {
                var server = fixture.server();
                var accepting = executor.submit(() -> {
                    server.serve();
                    return null;
                });
                try (var seller = new WireClient(server.localPort());
                     var bidder = new WireClient(server.localPort())) {
                    registerAndLogin(seller, "seller", Role.SELLER);
                    bidderId = registerAndLogin(bidder, "bidder", Role.BIDDER);
                    var itemId = (UUID) success(seller.call(Command.CREATE_ITEM,
                            new Request.CreateItemPayload(ItemType.ART, "Painting", "")));
                    var auction = (AuctionDetail) success(seller.call(Command.CREATE_AUCTION,
                            new Request.CreateAuctionPayload(itemId, BigDecimal.TEN,
                                    now.minusSeconds(1), now.plusSeconds(600))));
                    assertTrue(bidder.call(Command.LIST_AUCTIONS, new Request.EmptyPayload()).success());
                    var auctionPayload = new Request.AuctionIdPayload(auction.id());
                    assertEquals(auction, success(bidder.call(Command.GET_AUCTION, auctionPayload)));
                    assertEquals(ErrorCode.INVALID_BID, bidder.call(Command.PLACE_BID,
                            new Request.PlaceBidPayload(auction.id(), BigDecimal.ONE)).errorCode());
                    var placed = (AuctionDetail) success(bidder.call(Command.PLACE_BID,
                            new Request.PlaceBidPayload(auction.id(), new BigDecimal("12"))));
                    assertEquals(new BigDecimal("12"), placed.currentPrice());
                    time.set(now.plusSeconds(600));
                    fixture.scheduler().start();
                    // Poll the committed state while the scheduler advances independently of requests.
                    var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                    AuctionDetail detail;
                    do {
                        detail = (AuctionDetail) success(bidder.call(Command.GET_AUCTION, auctionPayload));
                        if (detail.winnerId() != null) {
                            break;
                        }
                        Thread.sleep(25);
                    } while (System.nanoTime() < deadline);
                    committed = detail;
                    assertEquals(AuctionStatus.FINISHED, committed.status());
                    assertEquals(bidderId, committed.winnerId());
                    assertEquals(ErrorCode.AUCTION_CLOSED, bidder.call(Command.PLACE_BID,
                            new Request.PlaceBidPayload(auction.id(), new BigDecimal("13"))).errorCode());
                    registerAndLogin(seller, "lateSeller", Role.SELLER);
                    savedItemId = (UUID) success(seller.call(Command.CREATE_ITEM,
                            new Request.CreateItemPayload(ItemType.ART, "Initial name", "")));
                    success(seller.call(Command.UPDATE_ITEM, new Request.UpdateItemPayload(
                            savedItemId, ItemType.ART, "Saved without a bid", "")));
                    deletedItemId = (UUID) success(seller.call(Command.CREATE_ITEM,
                            new Request.CreateItemPayload(ItemType.ART, "Deleted item", "")));
                    success(seller.call(Command.DELETE_ITEM, new Request.ItemIdPayload(deletedItemId)));
                }
                server.close();
                accepting.get(5, TimeUnit.SECONDS);
            }

            try (var fixture = PersistentServerFixture.open(loopback(0), store, clock)) {
                var restarted = fixture.server();
                var accepting = executor.submit(() -> {
                    restarted.serve();
                    return null;
                });
                try (var bidder = new WireClient(restarted.localPort())) {
                    var auctionPayload = new Request.AuctionIdPayload(committed.id());
                    assertEquals(ErrorCode.UNAUTHENTICATED, bidder.call(Command.GET_AUCTION, auctionPayload).errorCode());
                    var user = (UserSummary) success(bidder.call(Command.LOGIN,
                            new Request.LoginPayload("bidder", "password")));
                    assertEquals(bidderId, user.id());
                    assertEquals(committed, success(bidder.call(Command.GET_AUCTION, auctionPayload)));
                    success(bidder.call(Command.LOGIN, new Request.LoginPayload("lateSeller", "password")));
                    var item = assertInstanceOf(ItemDetail.class,
                            success(bidder.call(Command.GET_ITEM, new Request.ItemIdPayload(savedItemId))));
                    assertEquals("Saved without a bid", item.name());
                    assertEquals(ErrorCode.NOT_FOUND, bidder.call(Command.GET_ITEM,
                            new Request.ItemIdPayload(deletedItemId)).errorCode());
                }
                restarted.close();
                accepting.get(5, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedRegisterSaveReturnsErrorAndDoesNotPublishAccount() throws Exception {
        StateStore failingStore = new StateStore() {
            @Override
            public ApplicationState load() {
                return new ApplicationState();
            }

            @Override
            public void save(ApplicationState state) throws IOException {
                throw new IOException("Simulated snapshot write failure");
            }
        };

        var executor = Executors.newSingleThreadExecutor();
        try (var fixture = PersistentServerFixture.open(loopback(0), failingStore, Clock.systemUTC())) {
            var server = fixture.server();
            var accepting = executor.submit(() -> {
                server.serve();
                return null;
            });
            try (var client = new WireClient(server.localPort())) {
                var failed = client.call(Command.REGISTER, new Request.RegisterPayload("seller", "password", Role.SELLER));
                assertFalse(failed.success());
                assertEquals(ErrorCode.INTERNAL_ERROR, failed.errorCode());
                assertNull(failed.data());
                assertEquals(ErrorCode.AUTHENTICATION_FAILED, client.call(Command.LOGIN,
                        new Request.LoginPayload("seller", "password")).errorCode());
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
