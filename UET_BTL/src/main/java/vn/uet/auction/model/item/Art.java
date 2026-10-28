package vn.uet.auction.model.item;

import java.util.UUID;

public final class Art extends Item {
    public Art(UUID id, UUID sellerId, String name, String description) {
        super(id, sellerId, name, description);
    }

    @Override
    public ItemType getType() {
        return ItemType.ART;
    }
}
