package vn.uet.auction.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import vn.uet.auction.persistence.FileStateStore;
import vn.uet.auction.scheduler.AuctionScheduler;
import vn.uet.auction.persistence.StateStore;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.service.AuctionService;
import vn.uet.auction.service.ItemService;
import vn.uet.auction.service.StateTransactions;
import vn.uet.auction.service.UserService;

/** Starts the Socket server with the shared snapshot and auction scheduler. */
public final class ServerMain {
    private ServerMain() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 2) {
            throw new IllegalArgumentException("Usage: ServerMain [port] [snapshot-path]");
        }
        int port = args.length == 0 ? 5050 : Integer.parseInt(args[0]);
        Path snapshot = Path.of(args.length == 2 ? args[1] : "data/auction-state.bin");
        var services = configure(new FileStateStore(snapshot), Clock.systemUTC());
        var address = new InetSocketAddress(InetAddress.getLoopbackAddress(), port);

        try (var server = new AuctionServer(address, 16, services.dispatcher());
             var scheduler = new AuctionScheduler.Periodic(
                     services.auctions()::closeDueAuctions, Duration.ofSeconds(1))) {
            Thread shutdown = new Thread(() -> {
                scheduler.close();
                try {
                    server.close();
                } catch (IOException failure) {
                    System.getLogger(ServerMain.class.getName()).log(
                            System.Logger.Level.WARNING, "Could not close server listener", failure);
                }
            }, "auction-shutdown");
            Runtime.getRuntime().addShutdownHook(shutdown);
            try {
                scheduler.start();
                System.out.println("Auction server listening on loopback port " + server.localPort());
                server.serve();
            } finally {
                try {
                    Runtime.getRuntime().removeShutdownHook(shutdown);
                } catch (IllegalStateException shuttingDown) {
                    // The JVM already started executing shutdown hooks.
                }
            }
        }
    }

    static Services configure(StateStore store, Clock clock) throws IOException {
        var transactions = new StateTransactions(store);
        var lock = transactions.lock();
        var hasher = new PasswordHasher();
        return transactions.read(state -> {
            var users = new UserService(state, lock, hasher);
            var items = new ItemService(state, lock);
            var auctions = new AuctionService(clock, transactions, users, items);
            return new Services(new CommandDispatcher(users, items, lock, auctions, transactions, hasher), auctions);
        });
    }

    record Services(CommandDispatcher dispatcher, AuctionService auctions) {
    }
}
