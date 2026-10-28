package vn.uet.auction.factory;

import java.util.UUID;
import vn.uet.auction.model.item.Vehicle;

public final class VehicleCreator extends ItemCreator {
    @Override
    public Vehicle createItem(UUID id, UUID sellerId, String name, String description) {
        return new Vehicle(id, sellerId, name, description);
    }
}
