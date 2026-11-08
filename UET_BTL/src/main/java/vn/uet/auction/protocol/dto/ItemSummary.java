package vn.uet.auction.protocol.dto;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import vn.uet.auction.model.item.ItemType;

public record ItemSummary(
        UUID id,
        UUID sellerId,
        String name,
        ItemType type
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public ItemSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sellerId, "sellerId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }
}
