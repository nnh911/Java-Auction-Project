package vn.uet.auction.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import vn.uet.auction.exception.AuthenticationException;
import vn.uet.auction.model.user.*;
import vn.uet.auction.persistence.AccountState;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.protocol.dto.UserSummary;
import vn.uet.auction.security.PasswordHasher;

/** Week-7 in-memory API. Actor IDs must come from a trusted server session. */
public final class UserService {
    private final ApplicationState state;
    private final ReentrantLock lock;
    private final PasswordHasher hasher;

    public UserService(ApplicationState state, ReentrantLock lock, PasswordHasher hasher) {
        this.state = Objects.requireNonNull(state, "state");
        this.lock = Objects.requireNonNull(lock, "lock");
        this.hasher = Objects.requireNonNull(hasher, "hasher");
    }

    public UserSummary register(String username, String password, Role role) {
        Objects.requireNonNull(role, "role");
        if (role == Role.ADMIN) throw new SecurityException("Public registration cannot create Admin");
        User user = role == Role.SELLER ? new Seller(UUID.randomUUID(), username)
                : new Bidder(UUID.randomUUID(), username);
        return insert(user, password, false);
    }

    /** Trusted startup only; must never be exposed as a public command. */
    public UserSummary bootstrapAdmin(String username, String password) {
        return insert(new Admin(UUID.randomUUID(), username), password, true);
    }

    private UserSummary insert(User user, String password, boolean bootstrap) {
        if (password == null || password.isBlank()) throw new IllegalArgumentException("Password must not be blank");
        String hash = hasher.hash(password);
        return inLock(() -> {
            if (bootstrap && state.getUsers().values().stream().anyMatch(u -> u.getRole() == Role.ADMIN))
                throw new IllegalStateException("Admin already initialized");
            if (state.getUsers().values().stream().anyMatch(u -> u.getUsername().equals(user.getUsername())))
                throw new IllegalStateException("Username already exists");
            state.getUsers().put(user.getId(), user);
            state.getAccounts().put(user.getId(), new AccountState(hash, true));
            return summary(user);
        });
    }

    public UserSummary login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new AuthenticationException("Invalid credentials or inactive account");
        }
        String normalized = username.strip();
        return inLock(() -> {
            User user = state.getUsers().values().stream()
                    .filter(u -> u.getUsername().equals(normalized)).findFirst().orElse(null);
            AccountState account = user == null ? null : state.getAccounts().get(user.getId());
            if (account == null || !account.active() || !hasher.verify(password, account.passwordHash()))
                throw new AuthenticationException("Invalid credentials or inactive account");
            return summary(user);
        });
    }

    public List<UserSummary> listUsers(UUID actorId) {
        return inLock(() -> {
            requireRole(actorId, Role.ADMIN);
            return state.getUsers().values().stream().map(UserService::summary).toList();
        });
    }

    public void setUserActive(UUID actorId, UUID userId, boolean active) {
        inLock(() -> {
            requireRole(actorId, Role.ADMIN);
            User target = state.getUsers().get(Objects.requireNonNull(userId, "userId"));
            AccountState account = state.getAccounts().get(userId);
            if (target == null || account == null) throw new IllegalArgumentException("Account not found");
            if (!active && target.getRole() == Role.ADMIN && account.active()) {
                long admins = state.getUsers().values().stream().filter(u -> u.getRole() == Role.ADMIN)
                        .filter(u -> state.getAccounts().containsKey(u.getId()) && state.getAccounts().get(u.getId()).active()).count();
                if (admins <= 1) throw new IllegalStateException("Cannot disable the last active Admin");
            }
            state.getAccounts().put(userId, new AccountState(account.passwordHash(), active));
            return null;
        });
    }

    public UserSummary requireActive(UUID actorId) {
        return inLock(() -> {
            User user = state.getUsers().get(Objects.requireNonNull(actorId, "actorId"));
            AccountState account = state.getAccounts().get(actorId);
            if (user == null || account == null || !account.active()) throw new SecurityException("Active account required");
            return summary(user);
        });
    }

    public UserSummary requireRole(UUID actorId, Role role) {
        UserSummary user = requireActive(actorId);
        if (user.role() != Objects.requireNonNull(role, "role")) throw new SecurityException("Required role: " + role);
        return user;
    }

    private static UserSummary summary(User user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getRole());
    }

    private <T> T inLock(Supplier<T> work) {
        lock.lock();
        try { return work.get(); } finally { lock.unlock(); }
    }
}
