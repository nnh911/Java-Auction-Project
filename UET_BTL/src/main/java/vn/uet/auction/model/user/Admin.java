package vn.uet.auction.model.user;

import java.util.UUID;

public final class Admin extends User {
    public Admin(UUID id, String username) {
        super(id, username);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
