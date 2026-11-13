package vn.uet.auction.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Blocking accept loop with a bounded number of live connections. */
public final class AuctionServer implements AutoCloseable {
    private final ServerSocket listener;
    private final ThreadPoolExecutor workers;
    private final CommandDispatcher dispatcher;
    private final Set<Socket> clients = new HashSet<>();

    public AuctionServer(InetSocketAddress address, int maxClients, CommandDispatcher dispatcher)
            throws IOException {
        this.dispatcher = Objects.requireNonNull(dispatcher);
        Objects.requireNonNull(address);
        if (maxClients < 1) {
            throw new IllegalArgumentException("maxClients must be positive");
        }

        listener = new ServerSocket();
        try {
            listener.bind(address);
        } catch (IOException | RuntimeException failure) {
            listener.close();
            throw failure;
        }
        workers = new ThreadPoolExecutor(maxClients, maxClients, 0, TimeUnit.MILLISECONDS,
                new SynchronousQueue<>());
    }

    public int localPort() {
        return listener.getLocalPort();
    }

    /** Run once on a dedicated accept thread, or on the server's main thread. */
    public void serve() throws IOException {
        while (!listener.isClosed()) {
            Socket socket;
            try {
                socket = listener.accept();
            } catch (IOException failure) {
                if (listener.isClosed()) {
                    return;
                }
                throw failure;
            }
            startHandler(socket);
        }
    }

    private synchronized void startHandler(Socket socket) {
        if (listener.isClosed()) {
            closeSocket(socket);
            return;
        }
        clients.add(socket);
        try {
            workers.execute(() -> {
                try {
                    new ClientHandler(socket, dispatcher).run();
                } finally {
                    synchronized (this) {
                        clients.remove(socket);
                    }
                }
            });
        } catch (RejectedExecutionException saturated) {
            clients.remove(socket);
            closeSocket(socket);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        try {
            listener.close();
        } finally {
            clients.forEach(AuctionServer::closeSocket);
            workers.shutdown();
        }
    }

    private static void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
            // Continue closing the other connections.
        }
    }
}
