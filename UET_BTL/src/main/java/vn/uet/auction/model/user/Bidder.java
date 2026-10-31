package vn.uet.auction.model.user;

import java.util.UUID;

public final class Bidder extends User {
    private static final long serialVersionUID = 1L;

    public Bidder(UUID id, String username) {
        super(id, username);
    }

    @Override
    public Role getRole() {
        return Role.BIDDER;
    }
}
