package vn.uet.auction.item;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import vn.uet.auction.model.item.ItemType;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.service.*;

class ItemServiceTest {
    private final ApplicationState state = new ApplicationState();
    private final ReentrantLock lock = new ReentrantLock();
    private final UserService users = new UserService(state, lock, new PasswordHasher(100_000));
    private final ItemService items = new ItemService(state, lock);
    private final UUID seller = users.register("seller", "secret", Role.SELLER).id();

    @Test void sellerCrudAndReadSnapshots() {
        UUID bidder = users.register("bidder", "secret", Role.BIDDER).id();
        for (ItemType type : ItemType.values()) {
            UUID id = items.createItem(seller, type, "  Item  ", "  description  ");
            var before = items.getItem(bidder, id).orElseThrow();
            assertEquals(type, before.type()); assertEquals("Item", before.name());
            items.updateItem(seller, id, type, "New", "");
            assertEquals("New", items.getItem(seller, id).orElseThrow().name());
            assertEquals("Item", before.name());
            items.deleteItem(seller, id); assertTrue(items.getItem(seller, id).isEmpty());
        }
        assertTrue(items.listItems(seller).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> items.listItems(seller).clear());
    }

    @Test void forbiddenAndInvalidWritesLeaveStateUnchanged() {
        UUID other = users.register("other", "secret", Role.SELLER).id();
        UUID bidder = users.register("bidder", "secret", Role.BIDDER).id();
        UUID id = items.createItem(seller, ItemType.ART, "Painting", "");
        var original = items.getItem(seller, id);
        assertThrows(SecurityException.class, () -> items.createItem(bidder, ItemType.ART, "A", ""));
        assertThrows(SecurityException.class, () -> items.updateItem(other, id, ItemType.ART, "A", ""));
        assertThrows(SecurityException.class, () -> items.deleteItem(other, id));
        assertThrows(IllegalArgumentException.class, () -> items.updateItem(seller, id, ItemType.ART, " ", ""));
        assertThrows(NullPointerException.class, () -> items.updateItem(seller, id, null, "A", ""));
        assertEquals(original, items.getItem(seller, id));
        assertThrows(IllegalArgumentException.class, () -> items.deleteItem(seller, UUID.randomUUID()));
        UUID admin = users.bootstrapAdmin("admin", "secret").id();
        users.setUserActive(admin, seller, false);
        assertThrows(SecurityException.class, () -> items.listItems(seller));
        assertThrows(SecurityException.class, () -> items.getItem(seller, id));
        assertThrows(SecurityException.class, () -> items.deleteItem(seller, id));
        assertThrows(SecurityException.class, () -> items.updateItem(seller, id, ItemType.ART, "A", ""));
        assertThrows(SecurityException.class, () -> items.createItem(seller, ItemType.ART, "A", ""));
        assertEquals(1, state.getItems().size());
    }

    @Test void anyAuctionIncludingCanceledProtectsItemHistory() {
        Instant now = Instant.parse("2026-11-01T00:00:00Z");
        AuctionService auctions = new AuctionService(Clock.fixed(now, ZoneOffset.UTC), lock, state.getAuctions());
        UUID id = items.createItem(seller, ItemType.VEHICLE, "Car", "");
        assertEquals(seller, items.requireOwnedItem(seller, id).sellerId());
        var auction = auctions.createAuction(seller, id, BigDecimal.TEN, now, now.plusSeconds(60));
        assertThrows(IllegalStateException.class, () -> items.updateItem(seller, id, ItemType.ART, "A", ""));
        assertThrows(IllegalStateException.class, () -> items.deleteItem(seller, id));
        state.getAuctions().get(auction.id()).cancel();
        assertThrows(IllegalStateException.class, () -> items.deleteItem(seller, id));
        assertEquals("Car", items.getItem(seller, id).orElseThrow().name());
    }
}
