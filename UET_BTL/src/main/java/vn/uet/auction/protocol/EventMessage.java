// FILE KHUNG — CHƯA TRIỂN KHAI.
// Phụ trách: Thành viên 3. Tuần học kỳ: 6–10.
// Package dự kiến: vn.uet.auction.protocol
// Mục đích: DTO sự kiện gửi qua Socket; dữ liệu an toàn cho client.
// TODO: Nhóm tự viết package, khai báo kiểu, field, constructor và method.
// Thống nhất hợp đồng trước khi code; xem KE_HOACH_BTL_THEO_PDF.md.
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
        AUCTION_CANCELED
    }
}
