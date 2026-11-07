package vn.uet.auction.protocol.dto;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import vn.uet.auction.model.user.Role;

public record UserSummary(
        UUID id,
        String username,
        Role role
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public UserSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(role, "role");
    }
}
