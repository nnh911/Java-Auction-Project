package vn.uet.auction.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import vn.uet.auction.model.auction.Auction;
import vn.uet.auction.model.auction.AuctionStatus;
import vn.uet.auction.model.auction.Bid;

/**
 * Ảnh chụp bất biến của một phiên tại thời điểm đọc, an toàn để đưa ra ngoài lock.
 * Service không bao giờ trả đối tượng Auction sống. TV3 map record này sang DTO giao thức.
 *
 * highestBidderId: người đang dẫn đầu (null nếu chưa có bid).
 * winnerId: chỉ khác null khi phiên FINISHED/PAID và có bid (null KHÔNG có nghĩa là lỗi).
 */
public record AuctionSnapshot(
        UUID id, UUID itemId, UUID sellerId,
        BigDecimal startingPrice, BigDecimal currentPrice,
        Instant start, Instant end, AuctionStatus status,
        UUID highestBidderId, UUID winnerId, List<Bid> bids) {

    public AuctionSnapshot {
        bids = List.copyOf(bids);
    }

    public Optional<UUID> winner() { return Optional.ofNullable(winnerId); }

    public int bidCount() { return bids.size(); }

    static AuctionSnapshot of(Auction a) {
        return new AuctionSnapshot(
                a.getId(), a.getItemId(), a.getSellerId(),
                a.getStartingPrice(), a.getCurrentPrice(),
                a.getStart(), a.getEnd(), a.getStatus(),
                a.getHighestBid().map(Bid::getBidderId).orElse(null),
                a.getWinnerId().orElse(null),
                a.getHistory());
    }
}