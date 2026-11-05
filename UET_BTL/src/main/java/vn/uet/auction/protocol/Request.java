package vn.uet.auction.protocol;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import vn.uet.auction.model.item.ItemType;

public record Request(
        UUID requestId,
        Command command,
        Payload payload
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public Request {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(payload, "payload");
    }

    public sealed interface Payload extends Serializable
            permits LoginPayload, CreateItemPayload,
            CreateAuctionPayload, PlaceBidPayload, RegisterPayload, EmptyPayload,
            ItemIdPayload, UpdateItemPayload, SetUserActivePayload, AuctionIdPayload {
    }

    public record LoginPayload(
            String username,
            String password
    ) implements Payload {

        public LoginPayload {
            Objects.requireNonNull(username, "username");
            Objects.requireNonNull(password, "password");
        }

        @Override
        public String toString() {
            return "LoginPayload[credentials=REDACTED]";
        }
    }

    public record CreateItemPayload(
            ItemType type,
            String name,
            String description
    ) implements Payload {

        public CreateItemPayload {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(description, "description");
        }
    }

    public record CreateAuctionPayload(
            UUID itemId,
            BigDecimal startingPrice,
            Instant startTime,
            Instant endTime
    ) implements Payload {

        public CreateAuctionPayload {
            Objects.requireNonNull(itemId, "itemId");
            Objects.requireNonNull(startingPrice, "startingPrice");
            Objects.requireNonNull(startTime, "startTime");
            Objects.requireNonNull(endTime, "endTime");
        }
    }

    public record PlaceBidPayload(
            UUID auctionId,
            BigDecimal amount
    ) implements Payload {

        public PlaceBidPayload {
            Objects.requireNonNull(auctionId, "auctionId");
            Objects.requireNonNull(amount, "amount");
        }
    }

    public record EmptyPayload() implements Payload { }
    public record RegisterPayload(String username, String password,
            vn.uet.auction.model.user.Role role) implements Payload {
        public RegisterPayload {
            Objects.requireNonNull(username); Objects.requireNonNull(password); Objects.requireNonNull(role);
        }
        @Override public String toString() { return "RegisterPayload[credentials=REDACTED]"; }
    }
    public record ItemIdPayload(UUID itemId) implements Payload {
        public ItemIdPayload { Objects.requireNonNull(itemId); }
    }
    public record UpdateItemPayload(UUID itemId, ItemType type, String name, String description) implements Payload {
        public UpdateItemPayload {
            Objects.requireNonNull(itemId); Objects.requireNonNull(type);
            Objects.requireNonNull(name); Objects.requireNonNull(description);
        }
    }
    public record SetUserActivePayload(UUID userId, boolean active) implements Payload {
        public SetUserActivePayload { Objects.requireNonNull(userId); }
    }
    public record AuctionIdPayload(UUID auctionId) implements Payload {
        public AuctionIdPayload { Objects.requireNonNull(auctionId, "auctionId"); }
    }
}
