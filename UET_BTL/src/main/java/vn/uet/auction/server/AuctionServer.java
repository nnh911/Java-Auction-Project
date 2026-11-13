package vn.uet.auction.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Loopback transport for the week-9 application and integration tests. */
public final class AuctionServer implements AutoCloseable {
    private static final int MAX_CLIENTS = 16;

    private final ServerSocket listener;
    private final Set<Socket> clients = new HashSet<>();
    private final ExecutorService workers = Executors.newFixedThreadPool(MAX_CLIENTS);
    private final Thread acceptor;

    public AuctionServer(int port, CommandDispatcher dispatcher) throws IOException {
        listener = new ServerSocket(port, MAX_CLIENTS, InetAddress.getLoopbackAddress());
        acceptor = new Thread(() -> acceptClients(dispatcher), "auction-acceptor");
        acceptor.start();
    }

    public int port() {
        return listener.getLocalPort();
    }

    public void awaitTermination() throws InterruptedException {
        acceptor.join();
    }

    private void acceptClients(CommandDispatcher dispatcher) {
        try {
            while (!listener.isClosed()) {
                Socket socket = listener.accept();
                synchronized (clients) {
                    if (listener.isClosed() || clients.size() >= MAX_CLIENTS) {
                        closeSocket(socket);
                        continue;
                    }
                    clients.add(socket);
                    workers.execute(() -> {
                        try {
                            new ClientHandler(socket, dispatcher).run();
                        } finally {
                            synchronized (clients) {
                                clients.remove(socket);
                            }
                        }
                    });
                }
            }
        } catch (IOException e) {
            close();
        }
    }

    @Override
    public void close() {
        synchronized (clients) {
            try {
                listener.close();
            } catch (IOException e) {
                // Continue closing all connected clients.
            }
            clients.forEach(AuctionServer::closeSocket);
            workers.shutdown();
        }
    }

    private static void closeSocket(Socket socket) {
        try {
            socket.close();
        } catch (IOException e) {
            // The connection is being discarded.
        }
    }
}
