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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import vn.uet.auction.exception.AuctionClosedException;
import vn.uet.auction.exception.InvalidBidException;
import vn.uet.auction.model.auction.Auction;
import vn.uet.auction.model.auction.AuctionStatus;
import vn.uet.auction.model.auction.Bid;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.observer.AuctionEvent;
import vn.uet.auction.observer.AuctionListener;

/**
 * Logic nghiệp vụ đấu giá. Đây là NƠI DUY NHẤT được sửa đối tượng Auction.
 *
 * Khi tích hợp, các service chia sẻ ApplicationState và lock.
 * Không biết Singleton, JavaFX, Socket. Vì vậy test được bằng new AuctionService(fakeClock).
 *
 * Quy ước lock: mọi thao tác đọc-kiểm tra-ghi nằm trọn trong một lần giữ lock;
 * không trả đối tượng sống ra ngoài (chỉ trả AuctionSnapshot).
 * Caller không được giữ lock inject khi gọi public API: listener cần chạy sau unlock hoàn toàn.
 * Event trong từng thao tác theo thứ tự cập nhật; không cam kết thứ tự callback giữa các thread.
 */
public class AuctionService {

    /** Distinguishes business rejections while preserving the existing argument exception contract. */
    public static class Rejection extends IllegalArgumentException {
        private static final long serialVersionUID = 1L;
        public enum Reason { NOT_FOUND, FORBIDDEN, INVALID_BID, CONFLICT }

        private final Reason reason;

        protected Rejection(Reason reason, String message) {
            super(message);
            this.reason = reason;
        }

        public Reason reason() { return reason; }
    }

    private static final BigDecimal MAX_PRICE = new BigDecimal("1000000000000");
    private static final String PRICE_RANGE_MESSAGE = "Giá phải là số nguyên từ 1 đến " + MAX_PRICE + " VND";

    private static final System.Logger LOGGER = System.getLogger(AuctionService.class.getName());
    private final CopyOnWriteArrayList<AuctionListener> listeners = new CopyOnWriteArrayList<>();
    // Only accessed under lock. Each operation drains its events before releasing lock.
    private final List<AuctionEvent> pendingEvents = new ArrayList<>();
    private final Clock clock;
    private final ReentrantLock lock;
    private final Map<UUID, Auction> auctions;
    private final UserService userService;
    private final ItemService itemService;

    /** Dùng cho test và chạy độc lập: lock và dữ liệu riêng. */
    public AuctionService(Clock clock) {
        this(clock, new ReentrantLock(), new LinkedHashMap<>());
    }

    /** Lõi độc lập dành cho test nghiệp vụ không có tài khoản hoặc Item. */
    public AuctionService(Clock clock, ReentrantLock lock, Map<UUID, Auction> auctions) {
        this(clock, lock, auctions, null, null);
    }

    /** UserService và ItemService phải dùng cùng state và lock với AuctionService. */
    public AuctionService(Clock clock, ReentrantLock lock, ApplicationState state,
                          UserService userService, ItemService itemService) {
        this(clock, lock, Objects.requireNonNull(state, "state").getAuctions(),
                Objects.requireNonNull(userService, "userService"),
                Objects.requireNonNull(itemService, "itemService"));
    }

    private AuctionService(Clock clock, ReentrantLock lock, Map<UUID, Auction> auctions,
                           UserService userService, ItemService itemService) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.lock = Objects.requireNonNull(lock, "lock");
        this.auctions = Objects.requireNonNull(auctions, "auctions");
        this.userService = userService;
        this.itemService = itemService;
    }

    /**
     * Tạo phiên mới. Trạng thái ban đầu là OPEN, hoặc RUNNING nếu start đã qua.
     * Ném IllegalArgumentException khi dữ liệu sai, IllegalStateException khi Item đã có phiên
     * (tuần 8 sẽ đổi sang exception nghiệp vụ của nhóm).
     * Tích hợp TV1: requireOwnedItem phải chạy bên trong lock này, xuyên suốt tới khi thêm phiên.
     * Kiểm tra trước khi lấy lock có thể bị vô hiệu bởi sửa/xóa Item hoặc khóa tài khoản.
     */
    public AuctionSnapshot createAuction(UUID sellerId, UUID itemId, BigDecimal startingPrice,
                                         Instant start, Instant end) {
        Objects.requireNonNull(sellerId, "sellerId");
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(startingPrice, "startingPrice");
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        return inLock(() -> {
            if (itemService != null) {
                try {
                    itemService.requireOwnedItem(sellerId, itemId);
                } catch (IllegalArgumentException e) {
                    // With a non-null ID, this lookup uses IAE only for a missing Item.
                    throw new Rejection(Rejection.Reason.NOT_FOUND, "Không tìm thấy Item");
                }
            }
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
            if (!isValidPrice(startingPrice)) {
                throw new IllegalArgumentException(PRICE_RANGE_MESSAGE);
            }
            Auction auction = new Auction(UUID.randomUUID(), itemId, sellerId, startingPrice.setScale(0), start, end);
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
                StatusChange change = refresh(a, now);
                if (change != null) {
                    changes.add(change);
                }
            }
            return List.copyOf(changes);
        });
    }

    /**
     * Lõi đặt giá tuần 7: đọc giờ, kiểm tra và cập nhật trong cùng lock.
     * Actor phải lấy từ session tin cậy. Service tích hợp kiểm tra role/active trong lock.
     * Event phát sau unlock, kể cả chuyển trạng thái trước khi bid bị từ chối.
     * Phiên không RUNNING ném AuctionClosedException trước khi kiểm tra giá.
     * Giá ngoài khoảng, có phần lẻ hoặc không tăng ném InvalidBidException.
     */
    public AuctionSnapshot placeBid(UUID auctionId, UUID bidderId, BigDecimal amount) {
        Objects.requireNonNull(auctionId, "auctionId");
        Objects.requireNonNull(bidderId, "bidderId");
        Objects.requireNonNull(amount, "amount");
        return inLock(() -> {
            if (userService != null) {
                userService.requireRole(bidderId, Role.BIDDER);
            }
            Auction auction = requireAuction(auctionId);
            Instant now = clock.instant();
            refresh(auction, now);
            if (auction.getSellerId().equals(bidderId)) {
                throw new Rejection(Rejection.Reason.FORBIDDEN, "Chủ phiên không được tự đặt giá");
            }
            if (auction.getStatus() != AuctionStatus.RUNNING) {
                throw new AuctionClosedException("Phiên không nhận đặt giá: " + auction.getStatus());
            }
            if (!isValidPrice(amount)) {
                throw new InvalidBidException(PRICE_RANGE_MESSAGE);
            }
            if (amount.compareTo(auction.getCurrentPrice()) <= 0) {
                throw new InvalidBidException("Giá đặt phải lớn hơn giá hiện tại");
            }
            auction.acceptBid(new Bid(UUID.randomUUID(), auctionId, bidderId, amount.setScale(0), now));
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
            if (userService != null) {
                userService.requireActive(actorId);
            }
            Auction auction = requireAuction(auctionId);
            Instant now = clock.instant();
            refresh(auction, now);
            if (!auction.getWinnerId().filter(actorId::equals).isPresent()) {
                Rejection.Reason reason = auction.getWinnerId().isPresent()
                        ? Rejection.Reason.FORBIDDEN : Rejection.Reason.CONFLICT;
                throw new Rejection(reason, "Chỉ người thắng được xác nhận thanh toán");
            }
            AuctionStatus before = auction.getStatus();
            auction.markPaid();
            recordEvent(auction, before, now, AuctionEvent.Type.PAYMENT_CONFIRMED);
            return AuctionSnapshot.of(auction);
        });
    }

    /** Service tích hợp cho phép Admin active hoặc Seller active sở hữu phiên hủy. */
    public AuctionSnapshot cancelAuction(UUID actorId, UUID auctionId) {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(auctionId, "auctionId");
        return inLock(() -> {
            Auction auction = requireAuction(auctionId);
            if (userService != null) {
                Role role = userService.requireActive(actorId).role();
                boolean owner = role == Role.SELLER && auction.getSellerId().equals(actorId);
                if (role != Role.ADMIN && !owner) {
                    throw new SecurityException("Chỉ Admin hoặc Seller chủ phiên được hủy");
                }
            } else if (!auction.getSellerId().equals(actorId)) {
                throw new Rejection(Rejection.Reason.FORBIDDEN, "Chỉ chủ phiên được hủy trong chế độ độc lập");
            }
            Instant now = clock.instant();
            refresh(auction, now);
            AuctionStatus before = auction.getStatus();
            auction.cancel();
            recordEvent(auction, before, now, AuctionEvent.Type.AUCTION_CANCELED);
            return AuctionSnapshot.of(auction);
        });
    }

    private static boolean isValidPrice(BigDecimal price) {
        return price.signum() > 0 && price.compareTo(MAX_PRICE) <= 0
                && price.stripTrailingZeros().scale() <= 0;
    }

    private Auction requireAuction(UUID id) {
        Auction auction = auctions.get(id);
        if (auction == null) throw new Rejection(Rejection.Reason.NOT_FOUND, "Không tìm thấy phiên đấu giá");
        return auction;
    }

    private StatusChange refresh(Auction auction, Instant now) {
        AuctionStatus before = auction.getStatus();
        auction.refreshStatus(now);
        AuctionStatus after = auction.getStatus();
        if (before == after) {
            return null;
        }
        recordEvent(auction, before, now, after == AuctionStatus.RUNNING
                ? AuctionEvent.Type.AUCTION_STARTED : AuctionEvent.Type.AUCTION_FINISHED);
        return new StatusChange(auction.getId(), before, after, now);
    }

    private void recordEvent(Auction auction, AuctionStatus before, Instant now, AuctionEvent.Type type) {
        pendingEvents.add(new AuctionEvent(auction.getId(), before, auction.getStatus(), now,
                AuctionSnapshot.of(auction), type));
    }

    private <T> T inLock(Supplier<T> body) {
        // Reentrant acquisition would release only our hold, leaving the caller's hold active
        // during callbacks. Fail before any business mutation instead.
        if (lock.isHeldByCurrentThread()) {
            throw new IllegalStateException(
                    "Không gọi AuctionService khi caller đang giữ lock chung");
        }
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
