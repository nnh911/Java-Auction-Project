package vn.uet.auction.protocol.mapper;

import java.util.Objects;
import vn.uet.auction.observer.AuctionEvent;
import vn.uet.auction.protocol.EventMessage;
import vn.uet.auction.model.user.User;
import vn.uet.auction.model.item.Item;
import vn.uet.auction.model.auction.Bid;
import vn.uet.auction.service.AuctionSnapshot;
import vn.uet.auction.protocol.dto.*;

/** TV3 tuần 7: chuyển dữ liệu có sẵn sang DTO, không thực hiện nghiệp vụ. */
public final class ProtocolMapper {
    private ProtocolMapper() { }

    public static UserSummary toUserSummary(User user) {
        Objects.requireNonNull(user, "user");
        return new UserSummary(user.getId(), user.getUsername(), user.getRole());
    }

    public static ItemSummary toItemSummary(Item item) {
        Objects.requireNonNull(item, "item");
        return new ItemSummary(item.getId(), item.getSellerId(), item.getName(), item.getType());
    }

    public static ItemDetail toItemDetail(Item item) {
        Objects.requireNonNull(item, "item");
        return new ItemDetail(item.getId(), item.getSellerId(), item.getName(),
                item.getDescription(), item.getType());
    }

    public static BidSummary toBidSummary(Bid bid) {
        Objects.requireNonNull(bid, "bid");
        return new BidSummary(bid.getId(), bid.getAuctionId(), bid.getBidderId(),
                bid.getAmount(), bid.getPlacedAt());
    }

    public static AuctionSummary toAuctionSummary(AuctionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new AuctionSummary(snapshot.id(), snapshot.itemId(), snapshot.sellerId(),
                snapshot.currentPrice(), snapshot.status(), snapshot.start(), snapshot.end());
    }

    public static AuctionDetail toAuctionDetail(AuctionSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new AuctionDetail(snapshot.id(), snapshot.itemId(), snapshot.sellerId(),
                snapshot.startingPrice(), snapshot.currentPrice(), snapshot.status(),
                snapshot.start(), snapshot.end(), snapshot.winnerId(),
                snapshot.bids().stream().map(ProtocolMapper::toBidSummary).toList());
    }
    public static EventMessage toEventMessage(AuctionEvent event) {
        Objects.requireNonNull(event, "event");
        var type = switch (event.type()) {
            case BID_ACCEPTED -> EventMessage.EventType.BID_ACCEPTED;
            case AUCTION_STARTED -> EventMessage.EventType.AUCTION_STARTED;
            case AUCTION_FINISHED -> EventMessage.EventType.AUCTION_FINISHED;
            case AUCTION_CANCELED -> EventMessage.EventType.AUCTION_CANCELED;
            case PAYMENT_CONFIRMED -> EventMessage.EventType.PAYMENT_CONFIRMED;
        };
        return new EventMessage(type, event.auctionId(), event.occurredAt());
    }
}
