package vn.uet.auction.model.item;

import java.util.UUID;

public final class Electronics extends Item {
    private static final long serialVersionUID = 1L;

    public Electronics(UUID id, UUID sellerId, String name, String description) {
        super(id, sellerId, name, description);
    }

    @Override
    public ItemType getType() {
        return ItemType.ELECTRONICS;
    }
}
