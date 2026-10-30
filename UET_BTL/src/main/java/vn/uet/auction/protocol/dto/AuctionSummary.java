// FILE KHUNG — CHƯA TRIỂN KHAI.
// Phụ trách: Thành viên 3. Tuần học kỳ: 6–10.
// Package dự kiến: vn.uet.auction.protocol.dto
// Mục đích: Dữ liệu danh sách phiên; phối hợp TV2.
// TODO: Nhóm tự viết package, khai báo kiểu, field, constructor và method.
// Thống nhất hợp đồng trước khi code; xem KE_HOACH_BTL_THEO_PDF.md.
package vn.uet.auction.protocol.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import vn.uet.auction.model.auction.AuctionStatus;

/**
 * Một dòng trong danh sách phiên đấu giá (LIST_AUCTIONS). Bất biến, chỉ chứa dữ liệu an toàn.
 *
 * <p>Không có tên sản phẩm: client đối chiếu qua {@code itemId}. Cần bổ sung thì TV2/TV1 duyệt.
 */
// ĐỀ XUẤT FIELD: cần TV2 review.
public record AuctionSummary(
        UUID id,
        UUID itemId,
        UUID sellerId,
        BigDecimal currentPrice,
        AuctionStatus status,
        Instant startTime,
        Instant endTime
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public AuctionSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(sellerId, "sellerId");
        Objects.requireNonNull(currentPrice, "currentPrice");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(endTime, "endTime");
    }
}