package vn.uet.auction.factory;

import java.util.UUID;
import vn.uet.auction.model.item.Item;

public abstract class ItemCreator {
    public abstract Item createItem(
            UUID id, UUID sellerId, String name, String description);
}
