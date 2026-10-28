package vn.uet.auction.model.user;

import java.util.UUID;

public final class Seller extends User {
    public Seller(UUID id, String username) {
        super(id, username);
    }

    @Override
    public Role getRole() {
        return Role.SELLER;
    }
}
