package vn.uet.auction.auction;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;
import vn.uet.auction.model.item.ItemType;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.observer.AuctionEvent;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.service.ItemService;
import vn.uet.auction.service.UserService;
import vn.uet.auction.service.AuctionService;

class ConcurrentBiddingTest {
    @Test
    void concurrentBidsKeepHighestPriceAndStrictlyIncreasingHistory() throws Exception {
        Instant now = Instant.parse("2026-11-02T00:00:00Z");
        ApplicationState state = new ApplicationState();
        ReentrantLock lock = new ReentrantLock();
        UserService users = new UserService(state, lock, new PasswordHasher());
        ItemService items = new ItemService(state, lock);
        AuctionService service = new AuctionService(Clock.fixed(now, ZoneOffset.UTC), lock, state, users, items);
        UUID seller = users.register("seller", "test-password", Role.SELLER).id();
        UUID item = items.createItem(seller, ItemType.ART, "Painting", "");
        var auction = service.createAuction(seller, item, BigDecimal.ONE, now, now.plusSeconds(60));
        var events = new ConcurrentLinkedQueue<AuctionEvent>();
        service.addListener(event -> {
            assertFalse(lock.isHeldByCurrentThread());
            events.add(event);
        });
        int count = 32;
        var bidders = new ArrayList<UUID>();
        for (int i = 0; i < count; i++) {
            bidders.add(users.register("bidder_" + i, "test-password", Role.BIDDER).id());
        }
        ExecutorService pool = Executors.newFixedThreadPool(count);
        CountDownLatch ready = new CountDownLatch(count);
        CountDownLatch start = new CountDownLatch(1);
        var results = new ArrayList<Future<Boolean>>();
        try {
            for (int i = 0; i < count; i++) {
                BigDecimal amount = BigDecimal.valueOf(i + 2);
                UUID bidder = bidders.get(i);
                results.add(pool.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    try {
                        service.placeBid(auction.id(), bidder, amount);
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
            assertEquals(accepted, events.size());
            assertTrue(events.stream().allMatch(event -> event.type() == AuctionEvent.Type.BID_ACCEPTED));
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
