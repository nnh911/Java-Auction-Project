package vn.uet.auction.protocol.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import vn.uet.auction.model.auction.AuctionStatus;

/**
 * Chi tiết một phiên đấu giá và lịch sử bid (GET_AUCTION). Bất biến, chỉ chứa dữ liệu an toàn.
 *
 * <p>{@code winnerId} là field DUY NHẤT được phép null: null nghĩa là "chưa có người thắng"
 * (phiên chưa kết thúc, không có bid, hoặc bị hủy). Không dùng giá trị khác để biểu diễn việc này.
 *
 * <p>{@code history} theo thứ tự bid được chấp nhận (giá tăng dần); constructor sao chép
 * thành danh sách không sửa được, nên sửa danh sách gốc sau đó không ảnh hưởng DTO.
 */
// ĐỀ XUẤT FIELD: cần TV2 review.
public record AuctionDetail(
        UUID id,
        UUID itemId,
        UUID sellerId,
        BigDecimal startingPrice,
        BigDecimal currentPrice,
        AuctionStatus status,
        Instant startTime,
        Instant endTime,
        UUID winnerId,
        List<BidSummary> history
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public AuctionDetail {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(sellerId, "sellerId");
        Objects.requireNonNull(startingPrice, "startingPrice");
        Objects.requireNonNull(currentPrice, "currentPrice");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(startTime, "startTime");
        Objects.requireNonNull(endTime, "endTime");
        // winnerId được phép null (chưa có người thắng)
        Objects.requireNonNull(history, "history");
        history = List.copyOf(history); // ném NPE nếu có phần tử null
    }
}