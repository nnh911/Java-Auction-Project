package vn.uet.auction.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import vn.uet.auction.persistence.StateStore;
import vn.uet.auction.scheduler.AuctionScheduler;

/** Uses the same dispatcher and service wiring as ServerMain. */
public record PersistentServerFixture(AuctionServer server, AuctionScheduler.Periodic scheduler)
        implements AutoCloseable {

    public static PersistentServerFixture open(InetSocketAddress address, StateStore store, Clock clock)
            throws IOException {
        var services = ServerMain.configure(store, clock);
        var server = new AuctionServer(address, 4, services.dispatcher());
        var scheduler = new AuctionScheduler.Periodic(services.auctions()::closeDueAuctions, Duration.ofSeconds(1));
        return new PersistentServerFixture(server, scheduler);
    }

    @Override
    public void close() throws IOException {
        scheduler.close();
        server.close();
    }
}
