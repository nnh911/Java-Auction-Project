package vn.uet.auction.model.user;

import java.util.Objects;
import java.util.UUID;

public abstract class User {
    private final UUID id;
    private final String username;

    protected User(UUID id, String username) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(username, "username must not be null");
        String normalized = username.strip();
        if (!normalized.matches("[A-Za-z0-9_]{3,30}")) {
            throw new IllegalArgumentException(
                    "username must contain 3-30 letters, digits or underscores");
        }
        this.username = normalized;
    }

    public final UUID getId() {
        return id;
    }

    public final String getUsername() {
        return username;
    }

    public abstract Role getRole();
}
