package vn.uet.auction.auction;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import vn.uet.auction.service.AuctionService;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** TV4 hỗ trợ TV2: không sửa logic service của TV2. */
class ConcurrentBiddingTest {
    @Test
    @Disabled("Chờ ghép placeBid tuần 7 của TV2; review fixture quyền trước khi bật")
    void concurrentBidsKeepPriceAndHistoryConsistent() throws Exception {
        Instant now = Instant.parse("2026-11-05T00:00:00Z");
        AuctionService service = new AuctionService(new MutableClock(now));
        // Khi nối kiểm tra quyền, seed Seller/Item/Bidder qua service TV1 dưới lock chung.
        var auction = service.createAuction(UUID.randomUUID(), UUID.randomUUID(),
                new BigDecimal("100"), now, now.plusSeconds(60));
        int numberOfBids = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numberOfBids);
        CountDownLatch ready = new CountDownLatch(numberOfBids);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int index = 1; index <= numberOfBids; index++) {
                BigDecimal amount = BigDecimal.valueOf(100 + index);
                UUID bidderId = UUID.randomUUID();
                results.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Không nhận tín hiệu bắt đầu");
                    }
                    try {
                        service.placeBid(auction.id(), bidderId, amount);
                        return true;
                    } catch (IllegalArgumentException expected) {
                        // Giá thấp có thể thua giá cao đến trước. Review loại lỗi với TV2.
                        return false;
                    }
                }));
            }
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            int acceptedCount = 0;
            for (Future<Boolean> result : results) {
                if (result.get(5, TimeUnit.SECONDS)) { acceptedCount++; }
            }
            var snapshot = service.getAuction(auction.id()).orElseThrow();
            assertEquals(0, new BigDecimal("110").compareTo(snapshot.currentPrice()));
            assertEquals(acceptedCount, snapshot.bidCount());
            BigDecimal previous = new BigDecimal("100");
            Set<UUID> bidIds = new HashSet<>();
            for (var bid : snapshot.bids()) {
                assertTrue(bid.getAmount().compareTo(previous) > 0);
                assertTrue(bidIds.add(bid.getId()));
                previous = bid.getAmount();
            }
            assertEquals(0, previous.compareTo(snapshot.currentPrice()));
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void clockCanAdvanceAndShareTimeAcrossZones() {
        Instant now = Instant.parse("2026-11-05T00:00:00Z");
        MutableClock clock = new MutableClock(now);
        Clock vietnam = clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh"));
        clock.advance(Duration.ofMinutes(1));
        assertEquals(now.plusSeconds(60), vietnam.instant());
        assertEquals(ZoneId.of("Asia/Ho_Chi_Minh"), vietnam.getZone());
        clock.set(now);
        assertEquals(now, vietnam.instant());
    }
}
