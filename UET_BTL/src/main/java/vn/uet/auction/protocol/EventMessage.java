package vn.uet.auction.protocol;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EventMessage(
        EventType eventType,
        UUID auctionId,
        Instant occurredAt
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public EventMessage {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(auctionId, "auctionId");
        Objects.requireNonNull(occurredAt, "occurredAt");
    }

    public enum EventType {
        BID_ACCEPTED,
        AUCTION_STARTED,
        AUCTION_FINISHED,
        AUCTION_CANCELED,
        PAYMENT_CONFIRMED
    }
}
