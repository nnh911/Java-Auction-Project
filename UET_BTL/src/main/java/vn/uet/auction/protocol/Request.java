// FILE KHUNG — CHƯA TRIỂN KHAI.
// Phụ trách: Thành viên 3. Tuần học kỳ: 6–10.
// Package dự kiến: vn.uet.auction.protocol
// Mục đích: DTO request: nhóm thống nhất requestId, command và payload.
// TODO: Nhóm tự viết package, khai báo kiểu, field, constructor và method.
// Thống nhất hợp đồng trước khi code; xem KE_HOACH_BTL_THEO_PDF.md.
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
            CreateAuctionPayload, PlaceBidPayload {
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
}
