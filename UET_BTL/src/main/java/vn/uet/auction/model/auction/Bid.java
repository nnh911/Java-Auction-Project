package vn.uet.auction.model.auction;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Một lượt đặt giá đã được chấp nhận. Immutable: tạo xong không sửa được. */
public final class Bid implements Serializable {
    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final UUID auctionId;
    private final UUID bidderId;
    private final BigDecimal amount;
    private final Instant placedAt;

    public Bid(UUID id, UUID auctionId, UUID bidderId, BigDecimal amount, Instant placedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.auctionId = Objects.requireNonNull(auctionId, "auctionId");
        this.bidderId = Objects.requireNonNull(bidderId, "bidderId");
        this.amount = Objects.requireNonNull(amount, "amount");
        this.placedAt = Objects.requireNonNull(placedAt, "placedAt");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Số tiền bid phải > 0");
        }
    }

    public UUID getId() { return id; }
    public UUID getAuctionId() { return auctionId; }
    public UUID getBidderId() { return bidderId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getPlacedAt() { return placedAt; }

    @Override
    public boolean equals(Object o) {
        return o instanceof Bid other && id.equals(other.id);
    }

    @Override
    public int hashCode() { return id.hashCode(); }
}