// FILE KHUNG — CHƯA TRIỂN KHAI.
// Phụ trách: Thành viên 2. Tuần học kỳ: 7–12.
// Package dự kiến: vn.uet.auction.service
// Mục đích: Tạo phiên, đặt giá, vòng đời, winner, payment mô phỏng và cancel.
// TODO: Nhóm tự viết package, khai báo kiểu, field, constructor và method.
// Thống nhất hợp đồng trước khi code; xem KE_HOACH_BTL_THEO_PDF.md.
package vn.uet.auction.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import vn.uet.auction.model.auction.Auction;
import vn.uet.auction.model.auction.AuctionStatus;

/**
 * Logic nghiệp vụ đấu giá. Đây là NƠI DUY NHẤT được sửa đối tượng Auction.
 *
 * Phụ thuộc: chỉ Clock, một lock và một map dữ liệu, tất cả truyền qua constructor.
 * Không biết Singleton, JavaFX, Socket. Vì vậy test được bằng new AuctionService(fakeClock).
 *
 * Quy ước lock: mọi thao tác đọc-kiểm tra-ghi nằm trọn trong một lần giữ lock;
 * không trả đối tượng sống ra ngoài (chỉ trả AuctionSnapshot).
 */
public class AuctionService {

    private final Clock clock;
    private final ReentrantLock lock;
    private final Map<UUID, Auction> auctions;

    /** Dùng cho test và chạy độc lập: lock và dữ liệu riêng. */
    public AuctionService(Clock clock) {
        this(clock, new ReentrantLock(), new LinkedHashMap<>());
    }

    /**
     * Dùng khi ghép hệ thống: lock chung tạo ở nơi khởi tạo ứng dụng, map lấy từ
     * ApplicationState (TV4). Map chỉ được truy cập khi đang giữ lock này.
     */
    public AuctionService(Clock clock, ReentrantLock lock, Map<UUID, Auction> auctions) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.lock = Objects.requireNonNull(lock, "lock");
        this.auctions = Objects.requireNonNull(auctions, "auctions");
    }

    /**
     * Tạo phiên mới. Trạng thái ban đầu là OPEN, hoặc RUNNING nếu start đã qua.
     * Ném IllegalArgumentException khi dữ liệu sai, IllegalStateException khi Item đã có phiên
     * (tuần 8 sẽ đổi sang exception nghiệp vụ của nhóm).
     * TODO tuần 7: kiểm tra seller đúng role và sở hữu Item qua service của TV1 (gọi trước khi
     * lấy lock hoặc dùng lock chung để tránh deadlock).
     */
    public AuctionSnapshot createAuction(UUID sellerId, UUID itemId, BigDecimal startingPrice,
                                         Instant start, Instant end) {
        Objects.requireNonNull(sellerId, "sellerId");
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(startingPrice, "startingPrice");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        return inLock(() -> {
            Instant now = clock.instant();
            if (!end.isAfter(now)) {
                throw new IllegalArgumentException("Thời điểm kết thúc phải ở tương lai");
            }
            boolean itemTaken = auctions.values().stream()
                    .anyMatch(a -> a.getItemId().equals(itemId)
                            && a.getStatus() != AuctionStatus.CANCELED);
            if (itemTaken) {
                throw new IllegalStateException("Item đã có phiên đấu giá");
            }
            Auction auction = new Auction(UUID.randomUUID(), itemId, sellerId, startingPrice, start, end);
            auction.refreshStatus(now);
            auctions.put(auction.getId(), auction);
            return AuctionSnapshot.of(auction);
        });
    }

    /** Chỉ đọc: không đổi trạng thái, nên có thể chậm hơn đồng hồ tới khi closeDueAuctions chạy. */
    public Optional<AuctionSnapshot> getAuction(UUID auctionId) {
        Objects.requireNonNull(auctionId, "auctionId");
        return inLock(() -> Optional.ofNullable(auctions.get(auctionId)).map(AuctionSnapshot::of));
    }

    /** Chỉ đọc. Thứ tự theo lúc tạo. */
    public List<AuctionSnapshot> listAuctions() {
        return inLock(() -> auctions.values().stream().map(AuctionSnapshot::of).toList());
    }

    /**
     * Đưa mọi phiên về đúng trạng thái theo đồng hồ: OPEN->RUNNING, OPEN/RUNNING->FINISHED.
     * (Tên theo hợp đồng 3.3 của kế hoạch, dù hàm cũng thực hiện bước mở phiên.)
     * Trả về danh sách thay đổi; rỗng nếu không có gì đổi. Gọi lại ngay lập tức là vô hại.
     */
    public List<StatusChange> closeDueAuctions() {
        return inLock(() -> {
            Instant now = clock.instant();
            List<StatusChange> changes = new ArrayList<>();
            for (Auction a : auctions.values()) {
                AuctionStatus before = a.getStatus();
                a.refreshStatus(now);
                if (a.getStatus() != before) {
                    changes.add(new StatusChange(a.getId(), before, a.getStatus(), now));
                }
            }
            return List.copyOf(changes);
        });
    }

    /**
     * CHƯA VIẾT (tuần 7). Hợp đồng đã chốt, thứ tự kiểm tra BẮT BUỘC trong cùng một lần giữ lock:
     * 1) đọc now một lần, refreshStatus(now); 2) bidder hợp lệ và không phải seller;
     * 3) status == RUNNING, nếu không ném AuctionClosedException; 4) amount > currentPrice,
     * nếu không ném InvalidBidException; 5) acceptBid; 6) thu event, phát SAU khi unlock.
     * Nếu bước 1 làm phiên chuyển FINISHED thì bid bị từ chối nhưng vẫn phải phát AUCTION_FINISHED.
     */
    public AuctionSnapshot placeBid(UUID auctionId, UUID bidderId, BigDecimal amount) {
        throw new UnsupportedOperationException("Tuần 7: chưa cài đặt placeBid");
    }

    private <T> T inLock(Supplier<T> body) {
        lock.lock();
        try {
            return body.get();
        } finally {
            lock.unlock();
        }
    }
}