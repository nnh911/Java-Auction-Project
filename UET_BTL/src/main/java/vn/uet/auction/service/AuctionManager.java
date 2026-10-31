// FILE KHUNG — CHƯA TRIỂN KHAI.
// Phụ trách: Thành viên 2. Tuần học kỳ: 6–12.
// Package dự kiến: vn.uet.auction.service
// Mục đích: Singleton và điều phối nghiệp vụ; nhóm tự thiết kế lock/transaction.
// TODO: Nhóm tự viết package, khai báo kiểu, field, constructor và method.
// Thống nhất hợp đồng trước khi code; xem KE_HOACH_BTL_THEO_PDF.md.
package vn.uet.auction.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Singleton + Facade. Vai trò duy nhất: là cửa vào chung của hệ thống và nối dây production
 * (Clock thật, một AuctionService). KHÔNG chứa luật nghiệp vụ, KHÔNG giữ lock hay dữ liệu.
 *
 * Chỉ nơi khởi tạo ứng dụng (ServerMain, handler) được gọi getInstance().
 * AuctionService và model không được biết tới lớp này.
 */
public final class AuctionManager {

    /** Initialization-on-demand holder: lazy, thread-safe nhờ JVM, không cần synchronized mỗi lần gọi. */
    private static final class Holder {
        static final AuctionManager INSTANCE = new AuctionManager(new AuctionService(Clock.systemUTC()));
    }

    private final AuctionService service;

    private AuctionManager(AuctionService service) {
        this.service = service;
    }

    public static AuctionManager getInstance() {
        return Holder.INSTANCE;
    }

    public AuctionSnapshot createAuction(UUID sellerId, UUID itemId, BigDecimal startingPrice,
                                         Instant start, Instant end) {
        return service.createAuction(sellerId, itemId, startingPrice, start, end);
    }

    public Optional<AuctionSnapshot> getAuction(UUID auctionId) { return service.getAuction(auctionId); }

    public List<AuctionSnapshot> listAuctions() { return service.listAuctions(); }

    public List<StatusChange> closeDueAuctions() { return service.closeDueAuctions(); }

    public AuctionSnapshot placeBid(UUID auctionId, UUID bidderId, BigDecimal amount) {
        return service.placeBid(auctionId, bidderId, amount);
    }
}