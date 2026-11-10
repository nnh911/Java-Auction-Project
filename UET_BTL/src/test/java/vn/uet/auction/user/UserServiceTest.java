package vn.uet.auction.user;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import vn.uet.auction.exception.AuthenticationException;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.persistence.*;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.service.UserService;

class UserServiceTest {
    private final ApplicationState state = new ApplicationState();
    private final ReentrantLock lock = new ReentrantLock();
    private final UserService users = new UserService(state, lock, new PasswordHasher(100_000));

    @Test void registrationAndLogin() {
        var seller = users.register("  hoang  ", "secret", Role.SELLER);
        assertEquals(seller, users.login("hoang", "secret"));
        assertEquals("hoang", seller.username());
        assertNotEquals("secret", state.getAccounts().get(seller.id()).passwordHash());
        assertFalse(state.getAccounts().get(seller.id()).toString().contains("pbkdf2"));
        assertThrows(AuthenticationException.class, () -> users.login("hoang", "wrong"));
        assertThrows(AuthenticationException.class, () -> users.login(null, null));
        assertThrows(IllegalStateException.class, () -> users.register("hoang", "other", Role.BIDDER));
        assertThrows(SecurityException.class, () -> users.register("admin", "secret", Role.ADMIN));
        assertThrows(IllegalArgumentException.class, () -> users.register("new_user", " ", Role.BIDDER));
        assertEquals(1, state.getUsers().size());
    }

    @Test void validatesUsernameAndPasswordAtBoundaries() {
        for (String invalid : new String[]{"", "  ", "ab", "a".repeat(31), "bad-name"}) {
            assertThrows(IllegalArgumentException.class, () -> users.register(invalid, "secret", Role.BIDDER));
        }
        assertThrows(NullPointerException.class, () -> users.register(null, "secret", Role.BIDDER));
        assertThrows(NullPointerException.class, () -> users.register("valid", "secret", null));
        assertThrows(IllegalArgumentException.class, () -> users.register("valid", null, Role.BIDDER));
        assertThrows(IllegalArgumentException.class, () -> users.register("valid", "", Role.BIDDER));
        assertTrue(state.getAccounts().isEmpty());
        var shortest = users.register("abc", " secret ", Role.BIDDER);
        assertEquals(shortest, users.login("abc", " secret "));
        assertThrows(AuthenticationException.class, () -> users.login("abc", "secret"));
        assertEquals(30, users.register("x".repeat(30), "secret", Role.SELLER).username().length());
    }

    @Test void adminAndInactiveAccounts() {
        var admin = users.bootstrapAdmin("admin", "secret");
        var bidder = users.register("bidder", "secret", Role.BIDDER);
        assertThrows(IllegalStateException.class, () -> users.bootstrapAdmin("admin2", "secret"));
        assertThrows(SecurityException.class, () -> users.listUsers(bidder.id()));
        assertThrows(SecurityException.class, () -> users.setUserActive(bidder.id(), admin.id(), false));
        assertThrows(IllegalStateException.class, () -> users.setUserActive(admin.id(), admin.id(), false));
        users.setUserActive(admin.id(), bidder.id(), false);
        assertThrows(SecurityException.class, () -> users.login("bidder", "secret"));
        assertThrows(SecurityException.class, () -> users.requireActive(bidder.id()));
        users.setUserActive(admin.id(), bidder.id(), true);
        assertEquals(bidder, users.login("bidder", "secret"));
        assertEquals(2, users.listUsers(admin.id()).size());
        assertThrows(UnsupportedOperationException.class, () -> users.listUsers(admin.id()).clear());
        assertThrows(IllegalArgumentException.class, () -> users.setUserActive(admin.id(), UUID.randomUUID(), false));
    }

    @Test void credentialsAndActiveFlagSurviveMemoryCopy() throws Exception {
        var admin = users.bootstrapAdmin("admin", "secret");
        var seller = users.register("seller", "secret", Role.SELLER);
        MemoryStateStore store = new MemoryStateStore(); store.save(state);
        users.setUserActive(admin.id(), seller.id(), false);
        var loaded = store.load();
        var restored = new UserService(loaded, new ReentrantLock(), new PasswordHasher(100_000));
        assertEquals(seller, restored.login("seller", "secret"));
        store.save(state);
        assertThrows(SecurityException.class, () -> new UserService(store.load(), new ReentrantLock(),
                new PasswordHasher(100_000)).login("seller", "secret"));
        loaded.getAccounts().clear();
        assertEquals(2, store.load().getAccounts().size());
    }

    @Test void concurrentDuplicateRegistrationCommitsOnlyOneAccount() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> attempt = () -> {
            start.await();
            try { users.register("same_user", "secret", Role.BIDDER); return true; }
            catch (IllegalStateException expected) { return false; }
        };
        try {
            var a = pool.submit(attempt); var b = pool.submit(attempt); start.countDown();
            assertNotEquals(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
            assertEquals(1, state.getUsers().size()); assertEquals(1, state.getAccounts().size());
        } finally { pool.shutdownNow(); }
    }
}
