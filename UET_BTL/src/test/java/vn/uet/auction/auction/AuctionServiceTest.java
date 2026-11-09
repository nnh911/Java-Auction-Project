package vn.uet.auction.auction;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.uet.auction.exception.AuctionClosedException;
import vn.uet.auction.exception.InvalidBidException;
import vn.uet.auction.model.auction.AuctionStatus;
import vn.uet.auction.service.AuctionService;
import vn.uet.auction.service.AuctionSnapshot;
import vn.uet.auction.service.StatusChange;

class AuctionServiceTest {

    private static final Instant T0 = Instant.parse("2026-11-01T09:00:00Z");
    private static final Instant START = T0.plus(Duration.ofHours(1));
    private static final Instant END = T0.plus(Duration.ofHours(2));
    private static final BigDecimal PRICE = new BigDecimal("1000000");

    private MutableClock clock;
    private AuctionService service;
    private UUID seller;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(T0);
        service = new AuctionService(clock);
        seller = UUID.randomUUID();
    }

    private AuctionSnapshot create() {
        return service.createAuction(seller, UUID.randomUUID(), PRICE, START, END);
    }

    @Test
    void createdAuctionBeforeStartIsOpenAtStartingPrice() {
        AuctionSnapshot a = create();
        assertEquals(AuctionStatus.OPEN, a.status());
        assertEquals(PRICE, a.currentPrice());
        assertEquals(0, a.bidCount());
        assertTrue(a.winner().isEmpty());
    }

    @Test
    void createdAuctionWhoseStartHasPassedIsRunningImmediately() {
        AuctionSnapshot a = service.createAuction(seller, UUID.randomUUID(), PRICE,
                T0.minusSeconds(60), END);
        assertEquals(AuctionStatus.RUNNING, a.status());
    }

    @Test
    void createRejectsEndNotInFuture() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createAuction(seller, UUID.randomUUID(), PRICE, T0.minusSeconds(60), T0));
    }

    @Test
    void createRejectsBadPriceAndInvertedTimes() {
        assertThrows(IllegalArgumentException.class,
                () -> service.createAuction(seller, UUID.randomUUID(), BigDecimal.ZERO, START, END));
        assertThrows(IllegalArgumentException.class,
                () -> service.createAuction(seller, UUID.randomUUID(), PRICE, END, START));
    }

    @Test
    void sameItemCannotHaveTwoAuctions() {
        UUID item = UUID.randomUUID();
        service.createAuction(seller, item, PRICE, START, END);
        assertThrows(IllegalStateException.class,
                () -> service.createAuction(seller, item, PRICE, START, END));
    }

    @Test
    void tickMovesOpenToRunningExactlyAtStartAndIsIdempotent() {
        AuctionSnapshot a = create();

        clock.set(START.minusSeconds(1));
        assertTrue(service.closeDueAuctions().isEmpty());

        clock.set(START);                                        // now == start
        List<StatusChange> changes = service.closeDueAuctions();
        assertEquals(1, changes.size());
        assertEquals(a.id(), changes.get(0).auctionId());
        assertEquals(AuctionStatus.OPEN, changes.get(0).from());
        assertEquals(AuctionStatus.RUNNING, changes.get(0).to());

        assertTrue(service.closeDueAuctions().isEmpty());        // gọi lại: không đổi gì
    }

    @Test
    void tickFinishesExactlyAtEnd() {
        AuctionSnapshot a = create();
        clock.set(START);
        service.closeDueAuctions();

        clock.set(END.minusSeconds(1));
        assertTrue(service.closeDueAuctions().isEmpty());

        clock.set(END);                                          // now == end
        List<StatusChange> changes = service.closeDueAuctions();
        assertEquals(AuctionStatus.RUNNING, changes.get(0).from());
        assertEquals(AuctionStatus.FINISHED, changes.get(0).to());
        assertEquals(AuctionStatus.FINISHED, service.getAuction(a.id()).orElseThrow().status());
    }

    @Test
    void skippedTicksJumpOpenStraightToFinished() {          // ví dụ: server khởi động lại sau end
        AuctionSnapshot a = create();
        clock.set(END.plusSeconds(30));
        List<StatusChange> changes = service.closeDueAuctions();
        assertEquals(1, changes.size());
        assertEquals(AuctionStatus.OPEN, changes.get(0).from());
        assertEquals(AuctionStatus.FINISHED, changes.get(0).to());
        assertTrue(service.getAuction(a.id()).orElseThrow().winner().isEmpty());
    }

    @Test
    void readingDoesNotChangeStatusOnlyTickDoes() {
        AuctionSnapshot a = create();
        clock.set(START.plusSeconds(5));
        assertEquals(AuctionStatus.OPEN, service.getAuction(a.id()).orElseThrow().status());
        assertEquals(AuctionStatus.OPEN, service.listAuctions().get(0).status());
        service.closeDueAuctions();
        assertEquals(AuctionStatus.RUNNING, service.getAuction(a.id()).orElseThrow().status());
    }

    @Test
    void unknownAuctionIsEmptyNotAnError() {
        assertTrue(service.getAuction(UUID.randomUUID()).isEmpty());
    }

    @Test
    void listKeepsCreationOrderAndReturnsSnapshots() {
        AuctionSnapshot first = create();
        AuctionSnapshot second = create();
        List<AuctionSnapshot> list = service.listAuctions();
        assertEquals(List.of(first.id(), second.id()), list.stream().map(AuctionSnapshot::id).toList());
        assertThrows(UnsupportedOperationException.class, () -> list.add(first));
        assertThrows(UnsupportedOperationException.class, () -> first.bids().add(null));
    }

    @Test
    void twoServicesDoNotShareState() {
        create();
        AuctionService other = new AuctionService(new MutableClock(T0));
        assertTrue(other.listAuctions().isEmpty());
    }

    @Test
    void bidUpdatesHistoryAndWinnerAtEnd() {
        AuctionSnapshot a = create();
        UUID bidder = UUID.randomUUID();
        clock.set(START);
        AuctionSnapshot accepted = service.placeBid(a.id(), bidder, PRICE.add(BigDecimal.ONE));
        assertEquals(1, accepted.bidCount());
        assertEquals(START, accepted.bids().get(0).getPlacedAt());
        assertEquals(0, a.bidCount());
        assertTrue(accepted.winner().isEmpty());
        clock.set(END);
        assertThrows(AuctionClosedException.class,
                () -> service.placeBid(a.id(), bidder, PRICE.add(BigDecimal.TEN)));
        AuctionSnapshot finished = service.getAuction(a.id()).orElseThrow();
        assertEquals(AuctionStatus.FINISHED, finished.status());
        assertEquals(bidder, finished.winner().orElseThrow());
        assertEquals(1, finished.bidCount());
    }

    @Test
    void rejectedBidsDoNotChangePriceOrHistory() {
        AuctionSnapshot a = create();
        UUID bidder = UUID.randomUUID();
        assertThrows(AuctionClosedException.class, () -> service.placeBid(a.id(), bidder, PRICE));
        clock.set(START);
        assertThrows(IllegalArgumentException.class, () -> service.placeBid(a.id(), seller, PRICE));
        assertThrows(InvalidBidException.class, () -> service.placeBid(a.id(), bidder, PRICE));
        assertThrows(InvalidBidException.class,
                () -> service.placeBid(a.id(), bidder, BigDecimal.ZERO));
        assertThrows(NullPointerException.class, () -> service.placeBid(a.id(), bidder, null));
        assertThrows(IllegalArgumentException.class,
                () -> service.placeBid(UUID.randomUUID(), bidder, PRICE));
        AuctionSnapshot result = service.getAuction(a.id()).orElseThrow();
        assertEquals(PRICE, result.currentPrice());
        assertEquals(0, result.bidCount());
        // An exception must release the lock; a later valid bid still succeeds.
        assertEquals(1, service.placeBid(a.id(), bidder, PRICE.add(BigDecimal.ONE)).bidCount());
    }
    @Test
    void startingPriceMustBeWholeVndWithinInclusiveBounds() {
        BigDecimal max = new BigDecimal("1000000000000");
        var minimum = service.createAuction(seller, UUID.randomUUID(), new BigDecimal("1.00"), START, END);
        var maximum = service.createAuction(seller, UUID.randomUUID(), max, START, END);
        assertEquals(BigDecimal.ONE, minimum.startingPrice());
        assertEquals(max, maximum.startingPrice());

        for (BigDecimal invalid : List.of(BigDecimal.ZERO, BigDecimal.ONE.negate(),
                new BigDecimal("1000.01"), max.add(BigDecimal.ONE))) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.createAuction(seller, UUID.randomUUID(), invalid, START, END));
        }
        assertEquals(2, service.listAuctions().size());
    }

    @Test
    void bidPriceIsNormalizedAndClosedAuctionTakesPrecedenceOverInvalidPrice() {
        var auction = service.createAuction(seller, UUID.randomUUID(), BigDecimal.ONE, START, END);
        UUID bidder = UUID.randomUUID();
        BigDecimal max = new BigDecimal("1000000000000");
        BigDecimal aboveMax = max.add(BigDecimal.ONE);
        var events = new java.util.ArrayList<vn.uet.auction.observer.AuctionEvent.Type>();
        service.addListener(event -> events.add(event.type()));
        clock.set(START);
        var accepted = service.placeBid(auction.id(), bidder, new BigDecimal("1000.00"));
        assertEquals(new BigDecimal("1000"), accepted.currentPrice());
        assertEquals(accepted.currentPrice(), accepted.bids().get(0).getAmount());
        events.clear();

        for (BigDecimal invalid : List.of(new BigDecimal("1000.01"), aboveMax, new BigDecimal("1000.0"))) {
            assertThrows(InvalidBidException.class, () -> service.placeBid(auction.id(), bidder, invalid));
            assertEquals(accepted, service.getAuction(auction.id()).orElseThrow());
        }
        assertTrue(events.isEmpty());
        var atMax = service.placeBid(auction.id(), bidder, max);
        assertEquals(max, atMax.currentPrice());
        assertEquals(2, atMax.bidCount());
        events.clear();

        clock.set(END);
        assertThrows(AuctionClosedException.class,
                () -> service.placeBid(auction.id(), bidder, aboveMax.add(new BigDecimal("0.01"))));
        var finished = service.getAuction(auction.id()).orElseThrow();
        assertEquals(AuctionStatus.FINISHED, finished.status());
        assertEquals(atMax.bids(), finished.bids());
        assertEquals(max, finished.currentPrice());
        assertEquals(List.of(vn.uet.auction.observer.AuctionEvent.Type.AUCTION_FINISHED), events);
    }
}
