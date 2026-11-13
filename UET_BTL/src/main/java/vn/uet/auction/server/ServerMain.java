package vn.uet.auction.server;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.util.concurrent.locks.ReentrantLock;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.persistence.StateStore;
import vn.uet.auction.persistence.FileStateStore;
import vn.uet.auction.persistence.StateTransaction;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.service.AuctionService;
import vn.uet.auction.service.ItemService;
import vn.uet.auction.service.UserService;

public final class ServerMain {
    private ServerMain() { }

    public static AuctionServer start(int port, StateStore store, String adminPassword) throws IOException {
        var state = store.load();
        var lock = new ReentrantLock();
        var transaction = new StateTransaction(state, lock, store);
        var users = new UserService(state, lock, new PasswordHasher(), transaction);
        var items = new ItemService(state, lock, users, transaction);
        if (adminPassword != null && state.getUsers().values().stream().noneMatch(u -> u.getRole() == Role.ADMIN)) {
            users.bootstrapAdmin("admin", adminPassword);
        }
        var auctions = new AuctionService(Clock.systemUTC(), lock, state, users, items);
        return new AuctionServer(port, new CommandDispatcher(users, items, lock, auctions));
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 5555;
        Path path = Path.of(args.length > 1 ? args[1] : "data/auction.ser");
        try (var server = start(port, new FileStateStore(path), System.getenv("AUCTION_ADMIN_PASSWORD"))) {
            Runtime.getRuntime().addShutdownHook(new Thread(server::close));
            System.out.println("Auction server listening on loopback port " + server.port());
            server.awaitTermination();
        }
    }
}
