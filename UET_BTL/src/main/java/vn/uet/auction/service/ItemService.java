package vn.uet.auction.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import vn.uet.auction.factory.*;
import vn.uet.auction.model.item.Item;
import vn.uet.auction.model.item.ItemType;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.protocol.dto.ItemDetail;
import vn.uet.auction.protocol.dto.ItemSummary;
import vn.uet.auction.security.PasswordHasher;

/** Use the same State and lock as UserService and AuctionService. */
public final class ItemService {
    private final ApplicationState state;
    private final ReentrantLock lock;
    private final UserService users;

    public ItemService(ApplicationState state, ReentrantLock lock) {
        this.state = Objects.requireNonNull(state, "state");
        this.lock = Objects.requireNonNull(lock, "lock");
        this.users = new UserService(state, lock, new PasswordHasher());
    }

    public UUID createItem(UUID actorId, ItemType type, String name, String description) {
        return inLock(() -> {
            users.requireRole(actorId, Role.SELLER);
            Item item = create(UUID.randomUUID(), actorId, type, name, description);
            state.getItems().put(item.getId(), item);
            return item.getId();
        });
    }

    /** All active users can browse. Lists and detail DTOs are immutable snapshots. */
    public List<ItemSummary> listItems(UUID actorId) {
        return inLock(() -> {
            users.requireActive(actorId);
            return state.getItems().values().stream().map(i -> new ItemSummary(
                    i.getId(), i.getSellerId(), i.getName(), i.getType())).toList();
        });
    }

    public Optional<ItemDetail> getItem(UUID actorId, UUID itemId) {
        return inLock(() -> {
            users.requireActive(actorId);
            return Optional.ofNullable(state.getItems().get(Objects.requireNonNull(itemId, "itemId")))
                    .map(i -> new ItemDetail(i.getId(), i.getSellerId(), i.getName(), i.getDescription(), i.getType()));
        });
    }

    public void updateItem(UUID actorId, UUID itemId, ItemType type, String name, String description) {
        inLock(() -> {
            requireEditable(actorId, itemId);
            Item replacement = create(itemId, actorId, type, name, description);
            state.getItems().put(itemId, replacement);
            return null;
        });
    }

    public void deleteItem(UUID actorId, UUID itemId) {
        inLock(() -> {
            requireEditable(actorId, itemId);
            state.getItems().remove(itemId);
            return null;
        });
    }

    /** TV2 must hold the shared lock through this check AND auction insertion. */
    public ItemDetail requireOwnedItem(UUID actorId, UUID itemId) {
        return inLock(() -> {
            users.requireRole(actorId, Role.SELLER);
            ItemDetail item = getItem(actorId, itemId).orElseThrow(() -> new IllegalArgumentException("Item not found"));
            if (!item.sellerId().equals(actorId)) throw new SecurityException("Seller owner required");
            return item;
        });
    }

    private void requireEditable(UUID actorId, UUID itemId) {
        requireOwnedItem(actorId, itemId);
        if (state.getAuctions().values().stream().anyMatch(a -> a.getItemId().equals(itemId)))
            throw new IllegalStateException("Item already attached to an auction");
    }

    private static Item create(UUID id, UUID sellerId, ItemType type, String name, String description) {
        ItemCreator creator = switch (Objects.requireNonNull(type, "type")) {
            case ELECTRONICS -> new ElectronicsCreator();
            case ART -> new ArtCreator();
            case VEHICLE -> new VehicleCreator();
        };
        return creator.createItem(id, sellerId, name, description);
    }

    private <T> T inLock(Supplier<T> work) {
        lock.lock();
        try { return work.get(); } finally { lock.unlock(); }
    }
}
