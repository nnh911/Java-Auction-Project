package vn.uet.auction.auction;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.uet.auction.model.auction.AuctionStatus;
import vn.uet.auction.observer.AuctionEvent;
import vn.uet.auction.observer.AuctionListener;
import vn.uet.auction.scheduler.AuctionScheduler;
import vn.uet.auction.service.AuctionService;

class AuctionLifecycleTest {
    private final Instant start = Instant.parse("2026-11-02T00:00:00Z");
    private final UUID seller = UUID.randomUUID();
    private final UUID bidder = UUID.randomUUID();
    private MutableClock clock;
    private ReentrantLock lock;
    private AuctionService service;
    private UUID id;
    private ArrayList<AuctionEvent> events;

    @BeforeEach
    void setup() {
        clock = new MutableClock(start.minusSeconds(1));
        lock = new ReentrantLock();
        service = new AuctionService(clock, lock, new LinkedHashMap<>());
        id = service.createAuction(seller, UUID.randomUUID(), BigDecimal.ONE,
                start, start.plusSeconds(10)).id();
        events = new ArrayList<>();
        service.addListener(event -> {
            assertFalse(lock.isHeldByCurrentThread(), "Callbacks must run after unlock");
            events.add(event);
        });
    }

    @Test
    void winnerPaysOnceAfterFinishAndPaidCannotBeCanceled() {
        clock.set(start);
        service.placeBid(id, bidder, BigDecimal.TEN);
        assertThrows(IllegalArgumentException.class, () -> service.confirmPayment(bidder, id));
        clock.set(start.plusSeconds(10));
        assertThrows(IllegalArgumentException.class, () -> service.confirmPayment(seller, id));
        assertEquals(AuctionStatus.FINISHED, service.getAuction(id).orElseThrow().status());
        assertEquals(AuctionStatus.PAID, service.confirmPayment(bidder, id).status());
        assertThrows(IllegalStateException.class, () -> service.confirmPayment(bidder, id));
        assertThrows(IllegalStateException.class, () -> service.cancelAuction(seller, id));
        assertEquals(java.util.List.of(AuctionEvent.Type.AUCTION_STARTED,
                AuctionEvent.Type.BID_ACCEPTED, AuctionEvent.Type.AUCTION_FINISHED,
                AuctionEvent.Type.PAYMENT_CONFIRMED), events.stream().map(AuctionEvent::type).toList());
        assertEquals(AuctionStatus.RUNNING, events.get(1).snapshot().status());
        assertTrue(events.get(1).snapshot().winner().isEmpty());
    }

    @Test
    void ownerCanCancelOpenRunningOrFinishedAndNobodyCanReviveIt() {
        for (int offset : new int[] {-1, 0, 10}) {
            clock.set(start.minusSeconds(1));
            var auction = service.createAuction(seller, UUID.randomUUID(), BigDecimal.ONE,
                    start, start.plusSeconds(10));
            clock.set(start.plusSeconds(offset));
            assertThrows(IllegalArgumentException.class,
                    () -> service.cancelAuction(bidder, auction.id()));
            assertEquals(AuctionStatus.CANCELED, service.cancelAuction(seller, auction.id()).status());
            assertThrows(IllegalStateException.class, () -> service.cancelAuction(seller, auction.id()));
            assertThrows(IllegalStateException.class,
                    () -> service.placeBid(auction.id(), bidder, BigDecimal.TEN));
            assertThrows(IllegalArgumentException.class,
                    () -> service.confirmPayment(bidder, auction.id()));
        }
    }

    @Test
    void rejectedLateBidStillEmitsFinishWithoutBidAccepted() {
        clock.set(start.plusSeconds(10));
        assertThrows(IllegalStateException.class, () -> service.placeBid(id, bidder, BigDecimal.TEN));
        assertEquals(1, events.size());
        assertEquals(AuctionEvent.Type.AUCTION_FINISHED, events.get(0).type());
        assertTrue(events.get(0).snapshot().winner().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> service.confirmPayment(bidder, id));
        assertEquals(1, events.size());
    }

    @Test
    void failingListenerDoesNotStopOtherListenersOrRollBackAndRegistrationIsIdempotent() {
        AuctionListener broken = event -> { throw new IllegalStateException("test listener failure"); };
        service.addListener(broken);
        service.addListener(broken);
        int[] calls = {0};
        AuctionListener healthy = event -> calls[0]++;
        service.addListener(healthy);
        service.addListener(healthy);
        clock.set(start);
        assertEquals(1, service.placeBid(id, bidder, BigDecimal.TEN).bidCount());
        assertEquals(2, calls[0]);
        service.removeListener(broken);
        service.removeListener(healthy);
        assertThrows(IllegalArgumentException.class, () -> service.placeBid(id, bidder, BigDecimal.TEN));
        assertEquals(2, events.size());
        service.placeBid(id, bidder, BigDecimal.valueOf(11));
        assertEquals(2, calls[0]);
    }

    @Test
    void listenerMayReadServiceAndRemoveItself() {
        AuctionListener listener = new AuctionListener() {
            @Override public void onAuctionEvent(AuctionEvent event) {
                assertTrue(service.getAuction(event.auctionId()).isPresent());
                service.removeListener(this);
            }
        };
        service.addListener(listener);
        clock.set(start);
        AuctionScheduler scheduler = service::closeDueAuctions;
        assertEquals(1, scheduler.tick().size());
        assertTrue(scheduler.tick().isEmpty());
        assertEquals(1, events.size());
    }

    @Test
    void nullAndMissingIdentifiersAreRejected() {
        assertThrows(NullPointerException.class, () -> service.addListener(null));
        assertThrows(NullPointerException.class, () -> service.removeListener(null));
        assertThrows(NullPointerException.class, () -> service.confirmPayment(null, id));
        assertThrows(NullPointerException.class, () -> service.cancelAuction(seller, null));
        assertThrows(IllegalArgumentException.class, () -> service.confirmPayment(bidder, UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> service.cancelAuction(seller, UUID.randomUUID()));
    }
}
