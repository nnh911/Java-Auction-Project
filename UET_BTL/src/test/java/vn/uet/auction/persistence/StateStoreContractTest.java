package vn.uet.auction.persistence;

import org.junit.jupiter.api.Test;
import vn.uet.auction.model.auction.*;
import vn.uet.auction.model.item.Electronics;
import vn.uet.auction.model.user.Bidder;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.service.UserService;
import java.util.concurrent.locks.ReentrantLock;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

/** Mỗi test có State riêng. Snapshot State đang chạy phải lấy dưới lock chung. */
class StateStoreContractTest {
    @Test
    void newStoreIsEmpty() throws IOException {
        ApplicationState state = new MemoryStateStore().load();
        assertTrue(state.getAccounts().isEmpty());
        assertTrue(state.getUsers().isEmpty());
        assertTrue(state.getItems().isEmpty());
        assertTrue(state.getAuctions().isEmpty());
        assertTrue(state.getBids().isEmpty());
    }

    @Test
    void snapshotKeepsLockedAccountAndDoesNotShareAccountMap() throws IOException {
        ApplicationState original = new ApplicationState();
        PasswordHasher hasher = new PasswordHasher(100_000);
        UserService users = new UserService(original, new ReentrantLock(), hasher);
        UUID adminId = users.bootstrapAdmin("admin_test", "AdminTest123").id();
        String password = "MatKhauTest123";
        UUID bidderId = users.register("bidder_test", password, Role.BIDDER).id();
        users.setUserActive(adminId, bidderId, false);

        StateStore store = new MemoryStateStore();
        store.save(original);
        original.getAccounts().clear();

        ApplicationState loaded = store.load();
        AccountState account = loaded.getAccounts().get(bidderId);
        assertNotNull(account);
        assertFalse(account.active());
        assertNotEquals(password, account.passwordHash());
        assertTrue(hasher.verify(password, account.passwordHash()));

        loaded.getAccounts().clear();
        assertEquals(account, store.load().getAccounts().get(bidderId));
    }

    @Test
    void saveAndLoadPreserveData() throws IOException {
        ApplicationState original = createState();
        StateStore store = new MemoryStateStore();
        store.save(original);
        ApplicationState loaded = store.load();
        assertEquals(original.getUsers(), loaded.getUsers());
        assertEquals(original.getItems(), loaded.getItems());
        assertEquals(original.getBids(), loaded.getBids());
        Auction expected = original.getAuctions().values().iterator().next();
        Auction actual = loaded.getAuctions().get(expected.getId());
        assertNotSame(expected, actual);
        assertEquals(expected.getCurrentPrice(), actual.getCurrentPrice());
        assertEquals(expected.getHistory(), actual.getHistory());
        assertEquals(expected.getStatus(), actual.getStatus());
        assertEquals(expected.getStart(), actual.getStart());
        assertEquals(expected.getEnd(), actual.getEnd());
    }

    @Test
    void changingOriginalDoesNotChangeStore() throws IOException {
        ApplicationState original = createState();
        StateStore store = new MemoryStateStore();
        store.save(original);
        clearMaps(original);
        assertOneEntryPerMap(store.load());
    }

    @Test
    void changingLoadedMapsDoesNotChangeStore() throws IOException {
        StateStore store = new MemoryStateStore();
        store.save(createState());
        clearMaps(store.load());
        assertOneEntryPerMap(store.load());
    }

    @Test
    void auctionIsCopiedOnSaveAndLoad() throws IOException {
        ApplicationState original = createState();
        Auction auction = original.getAuctions().values().iterator().next();
        StateStore store = new MemoryStateStore();
        store.save(original);
        auction.cancel();
        Auction copy = store.load().getAuctions().get(auction.getId());
        assertEquals(AuctionStatus.RUNNING, copy.getStatus());
        copy.cancel();
        assertEquals(AuctionStatus.RUNNING,
                store.load().getAuctions().get(auction.getId()).getStatus());
    }

    @Test
    void storesAreIndependent() throws IOException {
        StateStore first = new MemoryStateStore();
        StateStore second = new MemoryStateStore();
        first.save(createState());
        assertOneEntryPerMap(first.load());
        assertTrue(second.load().getUsers().isEmpty());
    }

    @Test
    void nullCannotBeSaved() {
        StateStore store = new MemoryStateStore();
        assertThrows(IllegalArgumentException.class, () -> store.save(null));
    }

    @Test
    void fakeSaveFailureKeepsPreviousData() throws IOException {
        FailingStateStore store = new FailingStateStore();
        store.save(createState());
        store.failOnSave = true;
        assertThrows(IOException.class, () -> store.save(new ApplicationState()));
        assertOneEntryPerMap(store.load());
    }

    private ApplicationState createState() {
        // Fixture model, không đi qua luồng đăng nhập.
        ApplicationState state = new ApplicationState();
        UUID bidderId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        Instant now = Instant.parse("2026-11-05T00:00:00Z");
        state.getUsers().put(bidderId, new Bidder(bidderId, "bidder_test"));
        state.getItems().put(itemId,
                new Electronics(itemId, sellerId, "Laptop", "San pham mau"));
        Auction auction = new Auction(UUID.randomUUID(), itemId, sellerId,
                new BigDecimal("100"), now, now.plusSeconds(60));
        auction.refreshStatus(now);
        Bid bid = new Bid(UUID.randomUUID(), auction.getId(), bidderId,
                new BigDecimal("110"), now);
        auction.acceptBid(bid);
        state.getAuctions().put(auction.getId(), auction);
        // Chỉ mục bid được lấy từ history, không tạo bid độc lập.
        for (Bid accepted : auction.getHistory()) {
            state.getBids().put(accepted.getId(), accepted);
        }
        return state;
    }

    private void clearMaps(ApplicationState state) {
        state.getUsers().clear();
        state.getItems().clear();
        state.getAuctions().clear();
        state.getBids().clear();
    }

    private void assertOneEntryPerMap(ApplicationState state) {
        assertEquals(1, state.getUsers().size());
        assertEquals(1, state.getItems().size());
        assertEquals(1, state.getAuctions().size());
        assertEquals(1, state.getBids().size());
    }

    // Chỉ mô phỏng lỗi store; chưa chứng minh service có rollback.
    private static class FailingStateStore implements StateStore {
        private final MemoryStateStore delegate = new MemoryStateStore();
        private boolean failOnSave;

        @Override
        public ApplicationState load() throws IOException { return delegate.load(); }

        @Override
        public void save(ApplicationState state) throws IOException {
            if (failOnSave) {
                throw new IOException("Lỗi lưu giả lập");
            }
            delegate.save(state);
        }
    }
}
