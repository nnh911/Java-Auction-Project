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
