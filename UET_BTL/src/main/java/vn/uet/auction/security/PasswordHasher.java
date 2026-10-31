// FILE KHUNG — CHƯA TRIỂN KHAI.
// Phụ trách: Thành viên 4. Tuần học kỳ: 6–9.
// Package dự kiến: vn.uet.auction.security
// Mục đích: Nhóm tự triển khai hash có salt và verify; không lưu password thô.
// TODO: Nhóm tự viết package, khai báo kiểu, field, constructor và method.
// Thống nhất hợp đồng trước khi code; xem KE_HOACH_BTL_THEO_PDF.md.

package vn.uet.auction.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash and verify passwords using PBKDF2WithHmacSHA256.
 */
public final class PasswordHasher {

    public static final int DEFAULT_ITERATIONS = 600_000;

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final String PREFIX = "pbkdf2_sha256";

    private static final int SALT_BYTES = 16;

    private static final int HASH_BITS = 256;

    private static final int MIN_ITERATIONS = 100_000;

    private static final int MAX_ITERATIONS = 1_000_000;

    private final SecureRandom random = new SecureRandom();

    private final int iterations;

    public PasswordHasher() {
        this(DEFAULT_ITERATIONS);
    }

    public PasswordHasher(int iterations) {
        if (iterations < MIN_ITERATIONS
                || iterations > MAX_ITERATIONS) {
            throw new IllegalArgumentException(
                    "Invalid number of iterations");
        }

        this.iterations = iterations;
    }

    /**
     * Hash a password with a new random salt.
     */
    public String hash(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException(
                    "Password must not be null or empty");
        }

        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);

        byte[] hash = derive(password, salt, iterations, HASH_BITS);

        try {
            String encodedSalt = Base64.getEncoder().encodeToString(salt);

            String encodedHash = Base64.getEncoder().encodeToString(hash);

            return PREFIX + "$" + iterations
                    + "$" + encodedSalt
                    + "$" + encodedHash;

        } finally {
            Arrays.fill(hash, (byte) 0);
        }
    }

    /**
     * Verify a password against a stored hash.
     */
    public boolean verify(String password, String stored) {
        if (password == null || password.isEmpty()
                || stored == null) {
            return false;
        }

        byte[] actual = null;
        byte[] salt = null;
        byte[] expected = null;

        try {
            String[] parts = stored.split("\\$", -1);

            if (parts.length != 4 || !PREFIX.equals(parts[0])) {
                return false;
            }

            int storedIterations = Integer.parseInt(parts[1]);

            if (storedIterations < MIN_ITERATIONS || storedIterations > MAX_ITERATIONS) {
                return false;
            }

            salt = Base64.getDecoder().decode(parts[2]);
            expected = Base64.getDecoder().decode(parts[3]);

            if (salt.length != SALT_BYTES || expected.length != HASH_BITS / 8) {
                return false;
            }

            actual = derive(
                    password,
                    salt,
                    storedIterations,
                    HASH_BITS
            );

            return MessageDigest.isEqual(expected, actual);

        } catch (IllegalArgumentException e) {
            return false;

        } finally {
            if (actual != null) {
                Arrays.fill(actual, (byte) 0);
            }
            if (expected != null) {
                Arrays.fill(expected, (byte) 0);
            }
        }
    }

    /**
     * Generate a derived key using PBKDF2.
     */
    private static byte[] derive(
            String password,
            byte[] salt,
            int rounds,
            int bits) {

        char[] chars = password.toCharArray();

        PBEKeySpec spec = new PBEKeySpec(
                chars, salt, rounds, bits);

        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);

            return factory.generateSecret(spec).getEncoded();

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {

            throw new IllegalStateException(
                    "Cannot compute password hash", e);

        } finally {
            spec.clearPassword();
            Arrays.fill(chars, '\0');
        }
    }
}
