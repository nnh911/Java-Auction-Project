package vn.uet.auction.auction;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.uet.auction.model.auction.Auction;
import vn.uet.auction.model.auction.AuctionStatus;
import vn.uet.auction.model.auction.Bid;

class AuctionTest {

    private static final Instant START = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant END = Instant.parse("2026-11-01T11:00:00Z");

    private UUID auctionId;
    private Auction auction;

    @BeforeEach
    void setUp() {
        auctionId = UUID.randomUUID();
        auction = new Auction(auctionId, UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1000000"), START, END);
    }

    private Bid bid(String amount) {
        return new Bid(UUID.randomUUID(), auctionId, UUID.randomUUID(),
                new BigDecimal(amount), START.plusSeconds(60));
    }

    @Test
    void newAuctionIsOpenWithStartingPriceAndNoWinner() {
        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals(new BigDecimal("1000000"), auction.getCurrentPrice());
        assertTrue(auction.getHistory().isEmpty());
        assertTrue(auction.getWinnerId().isEmpty());
    }

    @Test
    void constructorRejectsBadInput() {
        UUID u = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class,
                () -> new Auction(u, u, u, BigDecimal.ZERO, START, END));
        assertThrows(IllegalArgumentException.class,
                () -> new Auction(u, u, u, BigDecimal.ONE, END, START));
        assertThrows(IllegalArgumentException.class,
                () -> new Auction(u, u, u, BigDecimal.ONE, START, START));
        assertThrows(NullPointerException.class,
                () -> new Auction(null, u, u, BigDecimal.ONE, START, END));
    }

    @Test
    void statusFollowsTimeIncludingBoundaries() {
        auction.refreshStatus(START.minusSeconds(1));
        assertEquals(AuctionStatus.OPEN, auction.getStatus());

        auction.refreshStatus(START);                       // now == start
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());

        auction.refreshStatus(END.minusSeconds(1));
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());

        auction.refreshStatus(END);                         // now == end
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    @Test
    void openCanJumpStraightToFinishedWhenTicksAreSkipped() {
        auction.refreshStatus(END.plusSeconds(5));
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
    }

    @Test
    void acceptedBidUpdatesPriceAndHistoryTogether() {
        auction.refreshStatus(START);
        Bid b1 = bid("1500000");
        Bid b2 = bid("1600000");
        auction.acceptBid(b1);
        auction.acceptBid(b2);

        assertEquals(new BigDecimal("1600000"), auction.getCurrentPrice());
        assertEquals(2, auction.getHistory().size());
        assertEquals(b2, auction.getHighestBid().orElseThrow());
    }

    @Test
    void bidNotHigherThanCurrentPriceIsRejectedAndLeavesNoTrace() {
        auction.refreshStatus(START);
        assertThrows(IllegalArgumentException.class, () -> auction.acceptBid(bid("1000000"))); // bằng giá khởi điểm
        auction.acceptBid(bid("1200000"));
        assertThrows(IllegalArgumentException.class, () -> auction.acceptBid(bid("1200000"))); // bằng giá hiện tại
        assertThrows(IllegalArgumentException.class, () -> auction.acceptBid(bid("1100000"))); // thấp hơn

        assertEquals(1, auction.getHistory().size());
        assertEquals(new BigDecimal("1200000"), auction.getCurrentPrice());
    }

    @Test
    void bidWhenNotRunningIsRejected() {
        assertThrows(IllegalStateException.class, () -> auction.acceptBid(bid("1500000"))); // còn OPEN
        auction.refreshStatus(END);
        assertThrows(IllegalStateException.class, () -> auction.acceptBid(bid("1500000"))); // FINISHED
    }

    @Test
    void bidForAnotherAuctionIsRejected() {
        auction.refreshStatus(START);
        Bid other = new Bid(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("1500000"), START);
        assertThrows(IllegalArgumentException.class, () -> auction.acceptBid(other));
    }

    @Test
    void winnerIsHighestBidderOnlyAfterFinish() {
        auction.refreshStatus(START);
        Bid top = bid("2000000");
        auction.acceptBid(bid("1500000"));
        auction.acceptBid(top);
        assertTrue(auction.getWinnerId().isEmpty());        // đang RUNNING

        auction.refreshStatus(END);
        assertEquals(top.getBidderId(), auction.getWinnerId().orElseThrow());
    }

    @Test
    void finishedWithoutBidsHasNoWinnerAndCannotBePaid() {
        auction.refreshStatus(END);
        assertTrue(auction.getWinnerId().isEmpty());
        assertThrows(IllegalStateException.class, auction::markPaid);
    }

    @Test
    void paidIsFinalAndCannotBeCanceled() {
        auction.refreshStatus(START);
        auction.acceptBid(bid("1500000"));
        auction.refreshStatus(END);
        auction.markPaid();
        assertEquals(AuctionStatus.PAID, auction.getStatus());

        assertThrows(IllegalStateException.class, auction::cancel);
        assertThrows(IllegalStateException.class, auction::markPaid);
        auction.refreshStatus(END.plusSeconds(100));         // tick không đổi PAID
        assertEquals(AuctionStatus.PAID, auction.getStatus());
    }

    @Test
    void canceledIsFinalAndClockCannotRevive() {
        auction.cancel();
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        auction.refreshStatus(START);
        assertEquals(AuctionStatus.CANCELED, auction.getStatus());
        assertThrows(IllegalStateException.class, auction::cancel);
        assertTrue(auction.getWinnerId().isEmpty());
    }

    @Test
    void historyIsASnapshotNotALiveView() {
        auction.refreshStatus(START);
        var snapshot = auction.getHistory();
        auction.acceptBid(bid("1500000"));
        assertTrue(snapshot.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(bid("1700000")));
    }
}