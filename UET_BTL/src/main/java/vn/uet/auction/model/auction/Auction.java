package vn.uet.auction.model.auction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Một phiên đấu giá. Lớp này chỉ giữ dữ liệu và bảo vệ invariant.
 * Quyền hạn (ai được bid/hủy/trả tiền) và lock do AuctionService xử lý.
 *
 * KHÔNG thread-safe: mọi lời gọi phải nằm trong lock của AuctionService.
 */
public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final UUID itemId;
    private final UUID sellerId;
    private final BigDecimal startingPrice;
    private final Instant start;
    private final Instant end;

    private BigDecimal currentPrice;
    private AuctionStatus status;
    private final List<Bid> history = new ArrayList<>();

    public Auction(UUID id, UUID itemId, UUID sellerId,
                   BigDecimal startingPrice, Instant start, Instant end) {
        this.id = Objects.requireNonNull(id, "id");
        this.itemId = Objects.requireNonNull(itemId, "itemId");
        this.sellerId = Objects.requireNonNull(sellerId, "sellerId");
        this.startingPrice = Objects.requireNonNull(startingPrice, "startingPrice");
        this.start = Objects.requireNonNull(start, "start");
        this.end = Objects.requireNonNull(end, "end");
        if (startingPrice.signum() <= 0) {
            throw new IllegalArgumentException("Giá khởi điểm phải > 0");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start phải trước end");
        }
        this.currentPrice = startingPrice;
        this.status = AuctionStatus.OPEN;
    }

    // ---------- getter (không có setter: đổi trạng thái qua method có kiểm tra) ----------

    public UUID getId() { return id; }
    public UUID getItemId() { return itemId; }
    public UUID getSellerId() { return sellerId; }
    public BigDecimal getStartingPrice() { return startingPrice; }
    public Instant getStart() { return start; }
    public Instant getEnd() { return end; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public AuctionStatus getStatus() { return status; }

    /** Bản sao tại thời điểm gọi, an toàn để đưa ra ngoài lock. */
    public List<Bid> getHistory() { return List.copyOf(history); }

    /** Bid cao nhất = bid cuối, vì mỗi bid được chấp nhận luôn lớn hơn bid trước. */
    public Optional<Bid> getHighestBid() {
        return history.isEmpty() ? Optional.empty() : Optional.of(history.get(history.size() - 1));
    }

    /** Chỉ có winner khi phiên đã đóng bình thường (FINISHED/PAID) và có ít nhất một bid. */
    public Optional<UUID> getWinnerId() {
        if (status != AuctionStatus.FINISHED && status != AuctionStatus.PAID) {
            return Optional.empty();
        }
        return getHighestBid().map(Bid::getBidderId);
    }

    // ---------- thay đổi trạng thái (package-private: chỉ AuctionService gọi) ----------

    /** Cập nhật OPEN/RUNNING/FINISHED theo giờ. Không đụng tới PAID/CANCELED. */
    void refreshStatus(Instant now) {
        Objects.requireNonNull(now, "now");
        if (status != AuctionStatus.OPEN && status != AuctionStatus.RUNNING) {
            return;
        }
        if (!now.isBefore(end)) {          // now >= end
            status = AuctionStatus.FINISHED;
        } else if (!now.isBefore(start)) { // start <= now < end
            status = AuctionStatus.RUNNING;
        }
        // now < start: giữ OPEN
    }

    /** Ghi nhận bid. Service phải gọi refreshStatus(now) trước đó. */
    void acceptBid(Bid bid) {
        Objects.requireNonNull(bid, "bid");
        if (status != AuctionStatus.RUNNING) {
            throw new IllegalStateException("Phiên không ở trạng thái RUNNING: " + status);
        }
        if (!bid.getAuctionId().equals(id)) {
            throw new IllegalArgumentException("Bid thuộc phiên khác");
        }
        if (bid.getAmount().compareTo(currentPrice) <= 0) {
            throw new IllegalArgumentException("Bid phải lớn hơn giá hiện tại");
        }
        history.add(bid);
        currentPrice = bid.getAmount();
    }

    /** FINISHED -> PAID. Việc kiểm tra "đúng winner" do service làm. */
    void markPaid() {
        if (status != AuctionStatus.FINISHED || history.isEmpty()) {
            throw new IllegalStateException("Chỉ thanh toán phiên FINISHED có người thắng");
        }
        status = AuctionStatus.PAID;
    }

    /** OPEN/RUNNING/FINISHED -> CANCELED. PAID và CANCELED không hủy được. */
    void cancel() {
        if (status == AuctionStatus.PAID || status == AuctionStatus.CANCELED) {
            throw new IllegalStateException("Không thể hủy phiên ở trạng thái " + status);
        }
        status = AuctionStatus.CANCELED;
    }
}