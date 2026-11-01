package vn.uet.auction.model.user;

import java.util.UUID;

public final class Seller extends User {
    private static final long serialVersionUID = 1L;

    public Seller(UUID id, String username) {
        super(id, username);
    }

    @Override
    public Role getRole() {
        return Role.SELLER;
    }
}
