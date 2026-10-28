package vn.uet.auction.factory;

import java.util.UUID;
import vn.uet.auction.model.item.Art;

public final class ArtCreator extends ItemCreator {
    @Override
    public Art createItem(UUID id, UUID sellerId, String name, String description) {
        return new Art(id, sellerId, name, description);
    }
}
