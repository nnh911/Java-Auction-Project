package vn.uet.auction.observer;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import vn.uet.auction.model.auction.AuctionStatus;
import vn.uet.auction.service.AuctionSnapshot;

/** Immutable event captured at commit time; no live Auction is exposed. */
public record AuctionEvent(UUID auctionId, AuctionStatus from, AuctionStatus to,
                           Instant occurredAt, AuctionSnapshot snapshot, Type type) {
    public enum Type {
        BID_ACCEPTED, AUCTION_STARTED, AUCTION_FINISHED, AUCTION_CANCELED, PAYMENT_CONFIRMED
    }

    public AuctionEvent {
        Objects.requireNonNull(auctionId, "auctionId");
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(type, "type");
        if (!auctionId.equals(snapshot.id()) || to != snapshot.status()) {
            throw new IllegalArgumentException("Event does not match snapshot");
        }
    }
}
