package vn.uet.auction.auction;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import vn.uet.auction.service.AuctionService;

class ConcurrentBiddingTest {
    @Test
    void concurrentBidsKeepHighestPriceAndStrictlyIncreasingHistory() throws Exception {
        Instant now = Instant.parse("2026-11-02T00:00:00Z");
        AuctionService service = new AuctionService(Clock.fixed(now, ZoneOffset.UTC));
        var auction = service.createAuction(UUID.randomUUID(), UUID.randomUUID(),
                BigDecimal.ONE, now, now.plusSeconds(60));
        int count = 32;
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        var results = new ArrayList<Future<Boolean>>();
        try {
            for (int i = 0; i < count; i++) {
                BigDecimal amount = BigDecimal.valueOf(i + 2);
                results.add(pool.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    try {
                        service.placeBid(auction.id(), UUID.randomUUID(), amount);
                        return true;
                    } catch (IllegalArgumentException rejectedLowerBid) {
                        return false;
                    }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            int accepted = 0;
            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) accepted++;
            }
            var snapshot = service.getAuction(auction.id()).orElseThrow();
            assertEquals(BigDecimal.valueOf(count + 1), snapshot.currentPrice());
            assertEquals(accepted, snapshot.bidCount());
            BigDecimal previous = BigDecimal.ONE;
            var ids = new java.util.HashSet<UUID>();
            for (var bid : snapshot.bids()) {
                assertTrue(bid.getAmount().compareTo(previous) > 0);
                assertTrue(ids.add(bid.getId()));
                assertEquals(auction.id(), bid.getAuctionId());
                previous = bid.getAmount();
            }
            assertEquals(snapshot.currentPrice(), previous);
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }
}
