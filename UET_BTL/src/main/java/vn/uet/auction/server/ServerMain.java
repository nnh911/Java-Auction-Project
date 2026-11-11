package vn.uet.auction.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.util.concurrent.locks.ReentrantLock;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.service.AuctionService;
import vn.uet.auction.service.ItemService;
import vn.uet.auction.service.UserService;

/** Runs the week 9 Socket integration with in-memory services. */
public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: ServerMain [port]");
        }
        int port = args.length == 0 ? 5050 : Integer.parseInt(args[0]);
        var state = new ApplicationState();
        var lock = new ReentrantLock();
        var users = new UserService(state, lock, new PasswordHasher());
        var items = new ItemService(state, lock);
        var auctions = new AuctionService(Clock.systemUTC(), lock, state, users, items);

        var address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
        try (var server = new AuctionServer(address, 16, new CommandDispatcher(users, items, lock, auctions))) {
            Thread shutdown = new Thread(() -> {
                try {
                    server.close();
                } catch (IOException failure) {
                    System.getLogger(ServerMain.class.getName()).log(
                            System.Logger.Level.WARNING, "Could not close server listener", failure);
                }
            }, "auction-shutdown");
            Runtime.getRuntime().addShutdownHook(shutdown);
            System.out.println("Auction server listening on loopback port " + server.localPort());
            server.serve();
        }
    }
}
