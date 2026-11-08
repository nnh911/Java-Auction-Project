package vn.uet.auction.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher(100_000);

    @Test
    void correctPasswordIsAccepted() {
        String stored = hasher.hash("MatKhau123");
        assertTrue(hasher.verify("MatKhau123", stored));
    }

    @Test
    void wrongPasswordIsRejected() {
        assertFalse(hasher.verify("SaiMatKhau", hasher.hash("MatKhau123")));
    }

    @Test
    void samePasswordProducesDifferentHashes() {
        String first = hasher.hash("MatKhau123");
        String second = hasher.hash("MatKhau123");
        assertNotEquals(first, second);
        assertTrue(hasher.verify("MatKhau123", first));
        assertTrue(hasher.verify("MatKhau123", second));
    }

    @Test
    void passwordSpacesArePreserved() {
        String stored = hasher.hash("  MatKhau123  ");
        assertTrue(hasher.verify("  MatKhau123  ", stored));
        assertFalse(hasher.verify("MatKhau123", stored));
    }

    @Test
    void invalidStoredHashIsRejected() {
        assertFalse(hasher.verify("123456", "khong-phai-hash"));
        assertFalse(hasher.verify("123456", "pbkdf2_sha256$abc$x$y"));
        assertFalse(hasher.verify("123456", null));
        assertFalse(hasher.verify(null, hasher.hash("123456")));
    }

    @Test
    void emptyPasswordCannotBeHashed() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(""));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));
    }
}
