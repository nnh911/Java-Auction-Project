// FILE KHUNG — CHƯA TRIỂN KHAI.
// Phụ trách: Thành viên 3. Tuần học kỳ: 6–10.
// Package dự kiến: vn.uet.auction.protocol.dto
// Mục đích: Dữ liệu một bid gửi cho client; phối hợp TV2.
// TODO: Nhóm tự viết package, khai báo kiểu, field, constructor và method.
// Thống nhất hợp đồng trước khi code; xem KE_HOACH_BTL_THEO_PDF.md.
package vn.uet.auction.protocol.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// ĐỀ XUẤT FIELD: cần TV2 review.
public record BidSummary(
        UUID id,
        UUID auctionId,
        UUID bidderId,
        BigDecimal amount,
        Instant acceptedAt
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public BidSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(auctionId, "auctionId");
        Objects.requireNonNull(bidderId, "bidderId");
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(acceptedAt, "acceptedAt");
    }
}
