package vn.uet.auction.client.network;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import vn.uet.auction.protocol.Command;
import vn.uet.auction.protocol.EventMessage;
import vn.uet.auction.protocol.Request;
import vn.uet.auction.protocol.Response;
import vn.uet.auction.protocol.dto.AuctionDetail;

/** One Socket connection. Create it on a background thread when using JavaFX. */
public final class AuctionClient implements AuctionGateway, AutoCloseable {
    private static final System.Logger LOGGER = System.getLogger(AuctionClient.class.getName());
    private static final ObjectInputFilter RESPONSE_FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=20;maxarray=32768;maxrefs=250000;maxbytes=33554432;"
            + "vn.uet.auction.protocol.Response;vn.uet.auction.protocol.ErrorCode;"
            + "vn.uet.auction.protocol.EventMessage;vn.uet.auction.protocol.EventMessage$*;"
            + "vn.uet.auction.protocol.dto.*;vn.uet.auction.model.user.Role;"
            + "vn.uet.auction.model.item.ItemType;vn.uet.auction.model.auction.AuctionStatus;"
            + "java.lang.Enum;java.lang.Object;java.util.UUID;java.util.ArrayList;java.util.CollSer;"
            + "java.util.ImmutableCollections$List12;java.util.ImmutableCollections$ListN;"
            + "java.math.BigDecimal;java.math.BigInteger;java.lang.Number;"
            + "java.time.Ser;java.time.Instant;!*");

    private final Socket socket = new Socket();
    private final ObjectOutputStream output;
    private final ObjectInputStream input;
    private final int requestTimeoutMillis;
    private final Semaphore requestSlots = new Semaphore(64);
    private final AtomicBoolean closed = new AtomicBoolean();
    private final ConcurrentHashMap<UUID, CompletableFuture<Response<? extends Serializable>>> pending =
            new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ClientEventListener> listeners = new CopyOnWriteArrayList<>();
    private final ThreadPoolExecutor writer = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(64), task -> daemon(task, "auction-client-writer"));

    public AuctionClient(InetSocketAddress address, int connectTimeoutMillis, int requestTimeoutMillis)
            throws IOException {
        try {
            if (connectTimeoutMillis <= 0 || requestTimeoutMillis <= 0) {
                throw new IllegalArgumentException("Timeouts must be positive");
            }
            this.requestTimeoutMillis = requestTimeoutMillis;
            socket.connect(Objects.requireNonNull(address), connectTimeoutMillis);
            socket.setSoTimeout(connectTimeoutMillis);
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            input.setObjectInputFilter(RESPONSE_FILTER);
            socket.setSoTimeout(0);
            daemon(this::receive, "auction-client-reader").start();
        } catch (IOException | RuntimeException failure) {
            disconnect(new IOException("Connection failed", failure));
            throw failure;
        }
    }

    // Internal until the group agrees on the public request API for the other controllers.
    CompletionStage<Response<? extends Serializable>> sendRequest(Command command, Request.Payload payload) {
        var request = new Request(UUID.randomUUID(), command, payload);
        if (closed.get() || !requestSlots.tryAcquire()) {
            return CompletableFuture.failedStage(new IOException("Connection closed or too many pending requests"));
        }
        var reply = new CompletableFuture<Response<? extends Serializable>>();
        pending.put(request.requestId(), reply);
        reply.whenComplete((response, failure) -> {
            pending.remove(request.requestId(), reply);
            requestSlots.release();
        });
        reply.orTimeout(requestTimeoutMillis, TimeUnit.MILLISECONDS);
        try {
            writer.execute(() -> write(request, reply));
        } catch (RejectedExecutionException failure) {
            reply.completeExceptionally(new IOException("Connection closed or request queue full", failure));
        }
        if (closed.get()) {
            reply.completeExceptionally(new IOException("Connection closed"));
        }
        return reply.minimalCompletionStage();
    }

    private void write(Request request, CompletableFuture<?> reply) {
        if (reply.isDone() || closed.get()) {
            return;
        }
        try {
            output.reset();
            output.writeObject(request);
            output.flush();
        } catch (IOException failure) {
            disconnect(failure);
        }
    }

    private void receive() {
        try {
            while (!closed.get()) {
                Object message = input.readObject();
                if (message instanceof Response<?> response) {
                    var reply = pending.remove(response.requestId());
                    if (reply != null) {
                        reply.complete(response);
                    }
                } else if (message instanceof EventMessage event) {
                    notifyListeners(event);
                } else {
                    throw new IOException("Unexpected protocol message");
                }
            }
        } catch (IOException | ClassNotFoundException failure) {
            disconnect(new IOException("Connection lost", failure));
        }
    }

    private void notifyListeners(EventMessage event) {
        for (ClientEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException failure) {
                LOGGER.log(System.Logger.Level.WARNING, "Auction event listener failed");
            }
        }
    }

    @Override
    public CompletionStage<AuctionDetail> getAuction(UUID auctionId) {
        return auctionRequest(Command.GET_AUCTION, new Request.AuctionIdPayload(auctionId));
    }

    @Override
    public CompletionStage<AuctionDetail> placeBid(UUID auctionId, BigDecimal amount) {
        return auctionRequest(Command.PLACE_BID, new Request.PlaceBidPayload(auctionId, amount));
    }

    @Override
    public CompletionStage<AuctionDetail> confirmPayment(UUID auctionId) {
        return auctionRequest(Command.CONFIRM_PAYMENT, new Request.AuctionIdPayload(auctionId));
    }

    @Override
    public CompletionStage<AuctionDetail> cancelAuction(UUID auctionId) {
        return auctionRequest(Command.CANCEL_AUCTION, new Request.AuctionIdPayload(auctionId));
    }

    private CompletionStage<AuctionDetail> auctionRequest(Command command, Request.Payload payload) {
        return sendRequest(command, payload).thenCompose(response -> {
            if (!response.success()) {
                return CompletableFuture.failedFuture(
                        new GatewayException(response.errorCode(), response.message()));
            }
            if (response.data() instanceof AuctionDetail detail) {
                return CompletableFuture.completedFuture(detail);
            }
            return CompletableFuture.failedFuture(new IOException("Unexpected auction response"));
        });
    }

    @Override
    public void addEventListener(ClientEventListener listener) {
        listeners.addIfAbsent(Objects.requireNonNull(listener));
    }

    @Override
    public void removeEventListener(ClientEventListener listener) {
        listeners.remove(Objects.requireNonNull(listener));
    }

    @Override
    public void close() {
        disconnect(new IOException("Connection closed"));
    }

    private void disconnect(IOException failure) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        writer.shutdownNow();
        try {
            socket.close();
        } catch (IOException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
        pending.forEach((id, reply) -> reply.completeExceptionally(failure));
    }

    private static Thread daemon(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }
}
