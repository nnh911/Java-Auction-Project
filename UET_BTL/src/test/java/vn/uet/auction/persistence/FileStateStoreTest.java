package vn.uet.auction.persistence;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import vn.uet.auction.model.auction.Auction;
import vn.uet.auction.model.auction.Bid;
import vn.uet.auction.model.item.ItemType;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.service.ItemService;
import vn.uet.auction.service.UserService;

import static org.junit.jupiter.api.Assertions.*;

class FileStateStoreTest {
    @TempDir Path directory;

    @Test
    void missingFileLoadsEmptyState() throws IOException {
        var state = new FileStateStore(directory.resolve("missing.bin")).load();
        assertTrue(state.getUsers().isEmpty());
        assertTrue(state.getAccounts().isEmpty());
        assertTrue(state.getItems().isEmpty());
        assertTrue(state.getAuctions().isEmpty());
        assertTrue(state.getBids().isEmpty());
    }

    @Test
    void restartKeepsAccountsItemsAuctionsAndBids() throws IOException {
        Path file = directory.resolve("state.bin");
        var state = new ApplicationState();
        var lock = new ReentrantLock();
        var hasher = new PasswordHasher(100_000);
        var users = new UserService(state, lock, hasher);
        var items = new ItemService(state, lock);
        var admin = users.bootstrapAdmin("admin_test", "Test123");
        var seller = users.register("seller_test", "Test123", Role.SELLER);
        var bidder = users.register("bidder_test", "Test123", Role.BIDDER);
        UUID itemId = items.createItem(seller.id(), ItemType.ART, "Painting", "");
        Instant now = Instant.parse("2026-11-12T00:00:00Z");
        var auction = new Auction(UUID.randomUUID(), itemId, seller.id(),
                BigDecimal.valueOf(100), now, now.plusSeconds(60));
        auction.refreshStatus(now);
        var bid = new Bid(UUID.randomUUID(), auction.getId(), bidder.id(),
                BigDecimal.valueOf(110), now);
        auction.acceptBid(bid);
        state.getAuctions().put(auction.getId(), auction);
        state.getBids().put(bid.getId(), bid);
        var store = new FileStateStore(file);
        store.save(state);
        users.setUserActive(admin.id(), bidder.id(), false);
        store.save(state);

        var loaded = new FileStateStore(file).load();
        var restoredUsers = new UserService(loaded, new ReentrantLock(), hasher);
        assertEquals(seller, restoredUsers.login("seller_test", "Test123"));
        assertThrows(SecurityException.class, () -> restoredUsers.login("bidder_test", "Test123"));
        var account = loaded.getAccounts().get(bidder.id());
        assertFalse(account.active());
        assertNotEquals("Test123", account.passwordHash());
        assertTrue(hasher.verify("Test123", account.passwordHash()));
        assertEquals("Painting", loaded.getItems().get(itemId).getName());
        var restoredAuction = loaded.getAuctions().get(auction.getId());
        assertEquals(BigDecimal.valueOf(110), restoredAuction.getCurrentPrice());
        assertEquals(1, restoredAuction.getHistory().size());
        assertEquals(bid.getId(), restoredAuction.getHistory().get(0).getId());
        assertEquals(bidder.id(), loaded.getBids().get(bid.getId()).getBidderId());
    }

    @Test
    void corruptFileIsReportedAndPreserved() throws IOException {
        Path file = directory.resolve("state.bin");
        byte[] original = {1, 2, 3};
        Files.write(file, original);
        assertThrows(IOException.class, () -> new FileStateStore(file).load());
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    void failedReplacementKeepsTargetAndRemovesTempFile() throws IOException {
        Path target = Files.createDirectory(directory.resolve("state.bin"));
        Path existing = Files.writeString(target.resolve("keep.txt"), "keep");
        assertThrows(IOException.class, () -> new FileStateStore(target).save(new ApplicationState()));
        assertEquals("keep", Files.readString(existing));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }
}
