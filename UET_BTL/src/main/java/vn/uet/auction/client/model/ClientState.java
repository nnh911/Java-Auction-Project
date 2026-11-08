package vn.uet.auction.client.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import vn.uet.auction.protocol.dto.AuctionDetail;
import vn.uet.auction.protocol.dto.AuctionSummary;

/**
 * TV3 tuần 7: dữ liệu danh sách/chi tiết cho prototype, chỉ giữ DTO.
 * Dùng trên một luồng UI; không gọi service, không quyết định luật đấu giá.
 */
public final class ClientState {
    private List<AuctionSummary> auctions = List.of();
    private UUID selectedAuctionId;
    private AuctionDetail selectedAuction;

    public List<AuctionSummary> getAuctions() {
        return auctions;
    }

    /** Giữ lựa chọn nếu ID còn tồn tại; xóa detail cũ khi refresh thành công. */
    public void replaceAuctions(List<AuctionSummary> summaries) {
        List<AuctionSummary> copy = List.copyOf(summaries);
        var ids = new HashSet<UUID>();
        for (AuctionSummary summary : copy) {
            if (!ids.add(summary.id())) {
                throw new IllegalArgumentException("Duplicate auction ID");
            }
        }
        auctions = copy;
        if (!ids.contains(selectedAuctionId)) {
            selectedAuctionId = null;
        }
        selectedAuction = null;
    }

    public Optional<UUID> getSelectedAuctionId() {
        return Optional.ofNullable(selectedAuctionId);
    }

    public Optional<AuctionDetail> getSelectedAuction() {
        return Optional.ofNullable(selectedAuction);
    }

    public void selectAuction(UUID auctionId) {
        Objects.requireNonNull(auctionId, "auctionId");
        if (auctions.stream().noneMatch(auction -> auction.id().equals(auctionId))) {
            throw new IllegalArgumentException("Auction is not in the current list");
        }
        if (!auctionId.equals(selectedAuctionId)) {
            selectedAuction = null;
        }
        selectedAuctionId = auctionId;
    }

    /** Bỏ qua response đến muộn của một phiên đã bỏ chọn. */
    public boolean acceptSelectedAuction(AuctionDetail detail) {
        Objects.requireNonNull(detail, "detail");
        if (!detail.id().equals(selectedAuctionId)) {
            return false;
        }
        selectedAuction = detail;
        return true;
    }

    public void clear() {
        auctions = List.of();
        selectedAuctionId = null;
        selectedAuction = null;
    }
}
