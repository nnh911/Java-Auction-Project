package vn.uet.auction.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.persistence.StateStore;
import vn.uet.auction.security.PasswordHasher;

/**
 * Singleton + Facade. Vai trò duy nhất: là cửa vào chung của hệ thống và nối dây production
 * (Clock thật, các service chia sẻ state và lock). Luật nghiệp vụ nằm trong service.
 *
 * Chỉ nơi khởi tạo ứng dụng (ServerMain, handler) được gọi getInstance().
 * AuctionService và model không được biết tới lớp này.
 */
public final class AuctionManager {

    /** Initialization-on-demand holder: lazy, thread-safe nhờ JVM, không cần synchronized mỗi lần gọi. */
    private static final class Holder {
        static final AuctionManager INSTANCE = new AuctionManager();
    }

    private final AuctionService service;
    private final ReentrantLock lock;

    private final UserService userService;
    private final ItemService itemService;

    private AuctionManager() {
        ApplicationState state = new ApplicationState();
        lock = new ReentrantLock();
        userService = new UserService(state, lock, new PasswordHasher());
        itemService = new ItemService(state, lock);
        service = new AuctionService(Clock.systemUTC(), lock, state, userService, itemService);
    }

    private AuctionManager(Clock clock, StateTransactions transactions) {
        lock = transactions.lock();
        userService = new UserService(transactions.state(), lock, new PasswordHasher());
        itemService = new ItemService(transactions.state(), transactions.lock());
        service = new AuctionService(clock, transactions, userService, itemService);
    }

    /** Load the server snapshot before accepting requests. */
    public static AuctionManager open(StateStore store, Clock clock) throws IOException {
        return new AuctionManager(clock, new StateTransactions(store));
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public AuctionService getAuctionService() {
        return service;
    }

    public UserService getUserService() {
        return userService;
    }

    public ItemService getItemService() {
        return itemService;
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
    public void addListener(vn.uet.auction.observer.AuctionListener listener) {
        service.addListener(listener);
    }

    public void removeListener(vn.uet.auction.observer.AuctionListener listener) {
        service.removeListener(listener);
    }

    public AuctionSnapshot confirmPayment(UUID actorId, UUID auctionId) {
        return service.confirmPayment(actorId, auctionId);
    }

    public AuctionSnapshot cancelAuction(UUID actorId, UUID auctionId) {
        return service.cancelAuction(actorId, auctionId);
    }
}
