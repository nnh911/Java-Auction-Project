package vn.uet.auction.service;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import vn.uet.auction.model.auction.AuctionStatus;

/** Một lần chuyển trạng thái do đồng hồ gây ra. Giữ làm kết quả tick; AuctionEvent được phát riêng cho listener. */
public record StatusChange(UUID auctionId, AuctionStatus from, AuctionStatus to, Instant at) {
    public StatusChange {
        Objects.requireNonNull(auctionId, "auctionId");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(at, "at");
    }
}