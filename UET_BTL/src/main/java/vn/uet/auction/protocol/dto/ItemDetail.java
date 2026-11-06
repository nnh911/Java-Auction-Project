package vn.uet.auction.protocol.dto;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import vn.uet.auction.model.item.ItemType;

public record ItemDetail(
        UUID id,
        UUID sellerId,
        String name,
        String description,
        ItemType type
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public ItemDetail {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sellerId, "sellerId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(type, "type");
    }
}
