// FILE KHUNG — CHƯA TRIỂN KHAI.
// Phụ trách: Thành viên 3. Tuần học kỳ: 6–10.
// Package dự kiến: vn.uet.auction.protocol.dto
// Mục đích: Thông tin user an toàn; phối hợp TV1, không có password/hash.
// TODO: Nhóm tự viết package, khai báo kiểu, field, constructor và method.
// Thống nhất hợp đồng trước khi code; xem KE_HOACH_BTL_THEO_PDF.md.
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
