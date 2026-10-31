// FILE KHUNG — CHƯA TRIỂN KHAI.
// Phụ trách: Thành viên 3. Tuần học kỳ: 6–10.
// Package dự kiến: vn.uet.auction.protocol.dto
// Mục đích: Dữ liệu chi tiết sản phẩm; phối hợp TV1.
// TODO: Nhóm tự viết package, khai báo kiểu, field, constructor và method.
// Thống nhất hợp đồng trước khi code; xem KE_HOACH_BTL_THEO_PDF.md.
package vn.uet.auction.protocol.dto;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import vn.uet.auction.model.item.ItemType;

public record ItemDetail(
        UUID id,
        UUID sellerId,
        String name,
        String description,
        ItemType type
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public ItemDetail {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(sellerId, "sellerId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(description, "description");
        Objects.requireNonNull(type, "type");
    }
}
