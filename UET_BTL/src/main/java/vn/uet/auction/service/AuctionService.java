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
import java.util.concurrent.CopyOnWriteArrayList;
import vn.uet.auction.observer.AuctionEvent;
import vn.uet.auction.observer.AuctionListener;
import java.util.function.Supplier;
import vn.uet.auction.model.auction.Auction;
import vn.uet.auction.model.auction.AuctionStatus;
import vn.uet.auction.model.auction.Bid;

/**
 * Logic nghiệp vụ đấu giá. Đây là NƠI DUY NHẤT được sửa đối tượng Auction.
 *
 * Phụ thuộc: chỉ Clock, một lock và một map dữ liệu, tất cả truyền qua constructor.
 * Không biết Singleton, JavaFX, Socket. Vì vậy test được bằng new AuctionService(fakeClock).
 *
 * Quy ước lock: mọi thao tác đọc-kiểm tra-ghi nằm trọn trong một lần giữ lock;
 * không trả đối tượng sống ra ngoài (chỉ trả AuctionSnapshot).
 * Caller không được giữ lock inject khi gọi public API: listener cần chạy sau unlock hoàn toàn.
 * Event trong từng thao tác theo thứ tự cập nhật; không cam kết thứ tự callback giữa các thread.
 */
public class AuctionService {

    private static final System.Logger LOGGER = System.getLogger(AuctionService.class.getName());
    private final CopyOnWriteArrayList<AuctionListener> listeners = new CopyOnWriteArrayList<>();
    // Only accessed under lock. Each operation drains its events before releasing lock.
    private final List<AuctionEvent> pendingEvents = new ArrayList<>();
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
            refresh(auction, now);
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
                refresh(a, now);
                if (a.getStatus() != before) {
                    changes.add(new StatusChange(a.getId(), before, a.getStatus(), now));
                }
            }
            return List.copyOf(changes);
        });
    }

    /**
     * Lõi đặt giá tuần 7: đọc giờ, kiểm tra và cập nhật trong cùng lock.
     * Caller phải xác thực bidder/role/active trước khi gọi; API TV1 chưa có.
     * Event phát sau unlock, kể cả chuyển trạng thái trước khi bid bị từ chối.
     * Lỗi tạm thời: null -> NPE, không tìm thấy/giá sai/tự bid -> IAE,
     * phiên không RUNNING -> ISE. Custom exceptions thuộc tuần 8.
     */
    public AuctionSnapshot placeBid(UUID auctionId, UUID bidderId, BigDecimal amount) {
        Objects.requireNonNull(auctionId, "auctionId");
        Objects.requireNonNull(bidderId, "bidderId");
        Objects.requireNonNull(amount, "amount");
        return inLock(() -> {
            Auction auction = auctions.get(auctionId);
            if (auction == null) {
                throw new IllegalArgumentException("Không tìm thấy phiên đấu giá");
            }
            Instant now = clock.instant();
            refresh(auction, now);
            if (auction.getSellerId().equals(bidderId)) {
                throw new IllegalArgumentException("Chủ phiên không được tự đặt giá");
            }
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                throw new IllegalStateException("Phiên không nhận đặt giá: " + auction.getStatus());
            }
            if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
                throw new IllegalArgumentException("Giá đặt phải lớn hơn giá hiện tại");
            }
            auction.acceptBid(new Bid(UUID.randomUUID(), auctionId, bidderId, amount, now));
            recordEvent(auction, auction.getStatus(), now, AuctionEvent.Type.BID_ACCEPTED);
            return AuctionSnapshot.of(auction);
        });
    }

    public void addListener(AuctionListener listener) {
        listeners.addIfAbsent(Objects.requireNonNull(listener, "listener"));
    }

    public void removeListener(AuctionListener listener) {
        listeners.remove(Objects.requireNonNull(listener, "listener"));
    }

    /** Caller supplies the authenticated actor ID. Only the winner can pay. */
    public AuctionSnapshot confirmPayment(UUID actorId, UUID auctionId) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(auctionId, "auctionId");
        return inLock(() -> {
            Auction auction = requireAuction(auctionId);
            Instant now = clock.instant();
            refresh(auction, now);
            if (!auction.getWinnerId().filter(actorId::equals).isPresent()) {
                throw new IllegalArgumentException("Chỉ người thắng được xác nhận thanh toán");
            }
            AuctionStatus before = auction.getStatus();
            auction.markPaid();
            recordEvent(auction, before, now, AuctionEvent.Type.PAYMENT_CONFIRMED);
            return AuctionSnapshot.of(auction);
        });
    }

    /** Owner path only; Admin authorization awaits TV1 trusted user lookup API. */
    public AuctionSnapshot cancelAuction(UUID actorId, UUID auctionId) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(auctionId, "auctionId");
        return inLock(() -> {
            Auction auction = requireAuction(auctionId);
            if (!auction.getSellerId().equals(actorId)) {
                throw new IllegalArgumentException("Chỉ chủ phiên được hủy qua API hiện tại");
            }
            Instant now = clock.instant();
            refresh(auction, now);
            AuctionStatus before = auction.getStatus();
            auction.cancel();
            recordEvent(auction, before, now, AuctionEvent.Type.AUCTION_CANCELED);
            return AuctionSnapshot.of(auction);
        });
    }

    private Auction requireAuction(UUID id) {
        Auction auction = auctions.get(id);
        if (auction == null) throw new IllegalArgumentException("Không tìm thấy phiên đấu giá");
        return auction;
    }

    private void refresh(Auction auction, Instant now) {
        AuctionStatus before = auction.getStatus();
        auction.refreshStatus(now);
        if (before != auction.getStatus()) {
            recordEvent(auction, before, now, auction.getStatus() == AuctionStatus.RUNNING
                    ? AuctionEvent.Type.AUCTION_STARTED : AuctionEvent.Type.AUCTION_FINISHED);
        }
    }

    private void recordEvent(Auction auction, AuctionStatus before, Instant now, AuctionEvent.Type type) {
        pendingEvents.add(new AuctionEvent(auction.getId(), before, auction.getStatus(), now,
                AuctionSnapshot.of(auction), type));
    }

    private <T> T inLock(Supplier<T> body) {
        List<AuctionEvent> events;
        lock.lock();
        try {
            return body.get();
        } finally {
            events = List.copyOf(pendingEvents);
            pendingEvents.clear();
            lock.unlock();
            for (AuctionEvent event : events) {
                for (AuctionListener listener : listeners) {
                    try {
                        listener.onAuctionEvent(event);
                    } catch (RuntimeException failure) {
                        LOGGER.log(System.Logger.Level.WARNING,
                                "Auction listener failed for " + event.type(), failure);
                    }
                }
            }
        }
    }
}
