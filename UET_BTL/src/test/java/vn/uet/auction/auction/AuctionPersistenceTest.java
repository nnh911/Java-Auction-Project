package vn.uet.auction.auction;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.uet.auction.exception.AuctionClosedException;
import vn.uet.auction.model.auction.AuctionStatus;
import vn.uet.auction.model.item.ItemType;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.observer.AuctionEvent;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.persistence.MemoryStateStore;
import vn.uet.auction.persistence.StateStore;
import vn.uet.auction.service.AuctionManager;

class AuctionPersistenceTest {
    private static final Instant START = Instant.parse("2026-11-12T10:00:00Z");
    private static final Instant END = START.plusSeconds(60);

    @Test
    void reloadPreservesBidAndSchedulerPersistsWinner() throws IOException {
        var store = new MemoryStateStore();
        var fixture = new Fixture(store);
        var bid = fixture.manager.placeBid(fixture.auction, fixture.bidder, BigDecimal.TEN);

        var reopened = AuctionManager.open(store, Clock.fixed(END, ZoneOffset.UTC));
        assertEquals(bid, reopened.getAuction(fixture.auction).orElseThrow());
        assertEquals(1, store.load().getBids().size());
        assertThrows(AuctionClosedException.class,
                () -> reopened.placeBid(fixture.auction, fixture.bidder, new BigDecimal("20")));
        assertEquals(AuctionStatus.RUNNING, reopened.getAuction(fixture.auction).orElseThrow().status());

        assertEquals(1, reopened.closeDueAuctions().size());
        var finished = AuctionManager.open(store, Clock.fixed(END, ZoneOffset.UTC))
                .getAuction(fixture.auction).orElseThrow();
        assertEquals(AuctionStatus.FINISHED, finished.status());
        assertEquals(fixture.bidder, finished.winner().orElseThrow());
        assertEquals(1, finished.bidCount());
    }

    @Test
    void failedSaveDiscardsBidAndEvent() throws IOException {
        var store = new FailingStore();
        var fixture = new Fixture(store);
        var before = fixture.manager.getAuction(fixture.auction).orElseThrow();
        var events = new ArrayList<AuctionEvent>();
        fixture.manager.addListener(events::add);

        store.fail = true;
        assertThrows(UncheckedIOException.class,
                () -> fixture.manager.placeBid(fixture.auction, fixture.bidder, BigDecimal.TEN));
        assertEquals(before, fixture.manager.getAuction(fixture.auction).orElseThrow());
        assertTrue(store.load().getBids().isEmpty());
        assertTrue(events.isEmpty());

        store.fail = false;
        fixture.manager.placeBid(fixture.auction, fixture.bidder, BigDecimal.TEN);
        assertEquals(1, store.load().getBids().size());
        assertEquals(1, events.size());
        assertEquals(AuctionEvent.Type.BID_ACCEPTED, events.get(0).type());
    }

    private static final class Fixture {
        private final AuctionManager manager;
        private final UUID bidder;
        private final UUID auction;

        private Fixture(StateStore store) throws IOException {
            manager = AuctionManager.open(store, Clock.fixed(START, ZoneOffset.UTC));
            var users = manager.getUserService();
            UUID seller = users.register("seller", "test-password", Role.SELLER).id();
            bidder = users.register("bidder", "test-password", Role.BIDDER).id();
            UUID item = manager.getItemService().createItem(seller, ItemType.ART, "Painting", "");
            auction = manager.createAuction(seller, item, BigDecimal.ONE, START, END).id();
        }
    }

    private static final class FailingStore implements StateStore {
        private final MemoryStateStore delegate = new MemoryStateStore();
        private boolean fail;

        @Override
        public ApplicationState load() throws IOException {
            return delegate.load();
        }

        @Override
        public void save(ApplicationState state) throws IOException {
            if (fail) {
                throw new IOException("Simulated write failure");
            }
            delegate.save(state);
        }
    }
}
