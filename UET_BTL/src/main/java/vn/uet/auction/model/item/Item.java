package vn.uet.auction.model.item;

import java.util.Objects;
import java.util.UUID;

public abstract class Item {
    private final UUID id;
    private final UUID sellerId;
    private final String name;
    private final String description;

    protected Item(UUID id, UUID sellerId, String name, String description) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.sellerId = Objects.requireNonNull(sellerId, "sellerId must not be null");
        Objects.requireNonNull(name, "name must not be null");
        String normalizedName = name.strip();
        if (normalizedName.isEmpty() || normalizedName.length() > 120) {
            throw new IllegalArgumentException("name must contain 1-120 characters");
        }
        Objects.requireNonNull(description, "description must not be null");
        String normalizedDescription = description.strip();
        if (normalizedDescription.length() > 2000) {
            throw new IllegalArgumentException("description must not exceed 2000 characters");
        }
        this.name = normalizedName;
        this.description = normalizedDescription;
    }

    public final UUID getId() {
        return id;
    }

    public final UUID getSellerId() {
        return sellerId;
    }

    public final String getName() {
        return name;
    }

    public final String getDescription() {
        return description;
    }

    public abstract ItemType getType();
}
