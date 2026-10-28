package vn.uet.auction.factory;

import java.util.UUID;
import vn.uet.auction.model.item.Electronics;

public final class ElectronicsCreator extends ItemCreator {
    @Override
    public Electronics createItem(
            UUID id, UUID sellerId, String name, String description) {
        return new Electronics(id, sellerId, name, description);
    }
}
