// FILE KIỂM THỬ KHUNG — CHƯA CÓ TEST CHẠY ĐƯỢC.
// Phụ trách: Thành viên 4. Tuần học kỳ: 7–12.
// Package dự kiến: vn.uet.auction.security
// Kịch bản cần tự viết: Hash có salt khác nhau, verify đúng/sai; không ghi password vào log.
// TODO: Tự khai báo package/class, JUnit test, fixture và assertion.
// Chưa có kết quả PASS hoặc coverage cho dự án khung này.

package vn.uet.auction.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher(100_000);

    @Test
    void testHashAndVerifyCorrectPassword() {
        String stored = hasher.hash("123456");

        assertTrue(hasher.verify("123456", stored));
    }

    @Test
    void testRejectWrongPassword() {
        String stored = hasher.hash("123456");

        assertFalse(hasher.verify("654321", stored));
    }

    @Test
    void testSamePasswordHasDifferentHashes() {
        String hash1 = hasher.hash("123456");
        String hash2 = hasher.hash("123456");

        assertNotEquals(hash1, hash2);
    }
}
