# Chữ ký API — TV3 (Lê Hải Thịnh), tuần 6

Cập nhật 2026-11-01 · Nguồn: nhánh `main` @ `195e041` (đã chứa toàn bộ `thinht6`, kiểm tra bằng `git merge-base`) · Schema chi tiết: [schema-v1.md](schema-v1.md) · Hợp đồng chung: [api-contract.md](api-contract.md)

Ký hiệu trạng thái: **[CÓ]** đã có trong code · **[THIẾU]** chưa có / còn là file khung · **[ĐỀ XUẤT]** chữ ký tôi đề xuất, chưa code, cần duyệt.

Mức kiểm chứng: toàn bộ `src/main/java` biên dịch được với `javac --release 17`; round-trip `ObjectOutputStream`/`ObjectInputStream` của Request, Response, AuctionDetail đạt (script riêng). **Chưa chạy** `ProtocolContractTest` bằng Maven (môi trường kiểm tra không tải được plugin Maven), nên không dùng tài liệu này làm bằng chứng test PASS.

---

## 1. API TV3 sở hữu — đã có trong code

Package `vn.uet.auction.protocol` (tất cả `Serializable`, `serialVersionUID = 1L`, bất biến, không null trừ field ghi rõ).

```java
public record Request(UUID requestId, Command command, Payload payload) implements Serializable
public sealed interface Request.Payload extends Serializable
        permits LoginPayload, CreateItemPayload, CreateAuctionPayload, PlaceBidPayload

public record LoginPayload(String username, String password)                  // toString che credentials
public record CreateItemPayload(ItemType type, String name, String description)
public record CreateAuctionPayload(UUID itemId, BigDecimal startingPrice, Instant startTime, Instant endTime)
public record PlaceBidPayload(UUID auctionId, BigDecimal amount)

public enum Command {
    REGISTER, LOGIN, LIST_USERS, SET_USER_ACTIVE,
    CREATE_ITEM, LIST_ITEMS, GET_ITEM, UPDATE_ITEM, DELETE_ITEM,
    CREATE_AUCTION, LIST_AUCTIONS, GET_AUCTION,
    PLACE_BID, CONFIRM_PAYMENT, CANCEL_AUCTION               // 15 command
}

public record Response<T extends Serializable>(
        UUID requestId, boolean success, ErrorCode errorCode, String message, T data) implements Serializable
    public static <T extends Serializable> Response<T> ok(UUID requestId, T data)
    public static <T extends Serializable> Response<T> fail(UUID requestId, ErrorCode errorCode, String message)

public enum ErrorCode {
    INVALID_REQUEST, UNSUPPORTED_COMMAND, AUTHENTICATION_FAILED, UNAUTHENTICATED,
    FORBIDDEN, NOT_FOUND, CONFLICT, INVALID_BID, AUCTION_CLOSED, INTERNAL_ERROR
}

public record EventMessage(EventType eventType, UUID auctionId, Instant occurredAt) implements Serializable
    public enum EventType { BID_ACCEPTED, AUCTION_STARTED, AUCTION_FINISHED, AUCTION_CANCELED }
```

Package `vn.uet.auction.protocol.dto`:

```java
public record UserSummary(UUID id, String username, Role role)
public record ItemSummary(UUID id, UUID sellerId, String name, ItemType type)
public record ItemDetail(UUID id, UUID sellerId, String name, String description, ItemType type)
public record AuctionSummary(UUID id, UUID itemId, UUID sellerId, BigDecimal currentPrice,
                             AuctionStatus status, Instant startTime, Instant endTime)
public record AuctionDetail(UUID id, UUID itemId, UUID sellerId, BigDecimal startingPrice,
                            BigDecimal currentPrice, AuctionStatus status, Instant startTime,
                            Instant endTime, UUID winnerId /* NULLABLE */, List<BidSummary> history)
public record BidSummary(UUID id, UUID auctionId, UUID bidderId, BigDecimal amount, Instant acceptedAt)
```

### Bất biến (đã kiểm chứng bằng code chạy)

| Kiểu | Quy tắc |
|---|---|
| `Request` | `requestId`, `command`, `payload` đều non-null → ném `NullPointerException` |
| `Response.ok` | `errorCode == null`, `data` non-null, `message == ""` |
| `Response.fail` | `errorCode` non-null, `message` không trắng, `data == null` |
| `AuctionDetail` | `winnerId` là field **duy nhất** được null (= chưa có người thắng); `history` được `List.copyOf` → sửa list gốc không ảnh hưởng, `add()` trên `history()` ném `UnsupportedOperationException` |
| Tiền | `BigDecimal`; `equals` phụ thuộc scale (`1500000` ≠ `1.5E+6`) → so sánh bằng `compareTo` |

---

## 2. API TV3 sở hữu — chưa code (tuần 9–12)

Toàn bộ file dưới đây hiện chỉ là khung 6 dòng comment (`server/*`, `client/network/*`). Chữ ký sau là **[ĐỀ XUẤT]** để nhóm duyệt trước khi viết.

```java
// vn.uet.auction.server
public final class Session {                          // danh tính do SERVER giữ sau login
    public boolean isAuthenticated();
    public UUID userId();                             // ném IllegalStateException nếu chưa login
    public Role role();
    public void bind(UserSummary user);               // gọi sau login thành công
    public void clear();                              // login lỗi / disconnect: xóa identity cũ
}

public class CommandDispatcher {
    /** Không bao giờ ném ra ngoài: mọi lỗi được map thành Response.fail(...). */
    public Response<? extends Serializable> dispatch(Session session, Request request);
}

// vn.uet.auction.client.network
public interface ClientEventListener {
    void onEvent(EventMessage event);                 // gọi trên luồng receiver, KHÔNG phải FX thread
    void onConnectionLost(Throwable cause);
}

public class AuctionClient implements AutoCloseable {
    public CompletableFuture<Response<? extends Serializable>> sendRequest(Command command,
                                                                           Request.Payload payload);
    public void addEventListener(ClientEventListener listener);
    public void removeEventListener(ClientEventListener listener);
    @Override public void close();
}
```

Nguyên tắc ràng buộc (đã có trong kế hoạch mục 3.4/tuần 9):
- Actor lấy từ `Session`, không lấy từ payload.
- `dispatch` không để exception lọt ra ngoài; không gửi stack trace cho client.
- `sendRequest` không chặn FX thread; caller đưa kết quả về giao diện bằng `Platform.runLater`.
- Response được ghép với request theo `requestId`, vì event có thể xen vào giữa.

---

## 3. API TV3 phụ thuộc vào — cần các bạn xác nhận

### 3.1 TV1 — User/Item (nhánh `main`)

| TV3 cần gọi | Trạng thái | Cần TV1 xác nhận |
|---|---|---|
| `register(username, password, Role)` | **[THIẾU]** `UserService` là file khung | Chữ ký; trả `User` hay `UserSummary`; chỉ cho `BIDDER`/`SELLER`? |
| `login(username, password)` | **[THIẾU]** | Kiểu trả về; ném `AuthenticationException` (hiện cũng là file khung) |
| `listUsers(actor)`, `setUserActive(actor, userId, active)` | **[THIẾU]** | `User` hiện chỉ có `id`, `username`, `getRole()` — chưa có `active`/hash: lưu ở đâu? |
| `createItem/listItems/getItem/updateItem/deleteItem` | **[THIẾU]** `ItemService` là file khung | Chữ ký có nhận `actorId`? Ai sinh UUID? |
| `ItemCreator.createItem(UUID id, UUID sellerId, String name, String description)` | **[CÓ]** + 3 subclass override | Giữ nguyên chữ ký |
| `User`: `getId()`, `getUsername()`, `getRole()`; `Item`: `getId()`, `getSellerId()`, `getName()`, `getDescription()`, `getType()` | **[CÓ]** | Là nguồn để map sang `UserSummary`/`ItemSummary`/`ItemDetail` |

### 3.2 TV2 — Auction

| TV3 cần gọi | Trạng thái | Cần TV2 xác nhận |
|---|---|---|
| `AuctionService/AuctionManager.createAuction(UUID sellerId, UUID itemId, BigDecimal startingPrice, Instant start, Instant end)` → `AuctionSnapshot` | **[CÓ]** | Giữ chữ ký; hiện ném `IllegalArgumentException`/`IllegalStateException` (tuần 8 đổi sang exception nghiệp vụ) |
| `getAuction(UUID)` → `Optional<AuctionSnapshot>`; `listAuctions()` → `List<AuctionSnapshot>` | **[CÓ]** | `NOT_FOUND` map từ `Optional.empty()` — đồng ý? |
| `closeDueAuctions()` → `List<StatusChange>` | **[CÓ]** | Là nguồn để TV3 phát `AUCTION_STARTED`/`AUCTION_FINISHED` |
| `placeBid(UUID auctionId, UUID bidderId, BigDecimal amount)` → `AuctionSnapshot` | **[THIẾU]** hiện luôn ném `UnsupportedOperationException` | Hạn giao (tuần 7); thứ tự exception: `AuctionClosedException` trước `InvalidBidException` |
| `confirmPayment(...)`, `cancelAuction(...)` | **[THIẾU]** không có trong `AuctionService` lẫn `AuctionManager` (model chỉ có `Auction.markPaid()`/`cancel()`) | Chữ ký, tham số `actorId`, ai kiểm tra quyền (winner/owner/Admin) |
| Observer: `addListener`/`removeListener`, `AuctionEvent` | **[THIẾU]** file khung | Kiểu event; phát sau unlock |
| Wiring | `AuctionManager` đang tạo `AuctionService` với map/lock riêng, **chưa nối `ApplicationState`** | Khi nào nối State/lock chung với TV4 |

### 3.3 TV4 — Persistence/Security

| TV3 cần | Trạng thái | Cần TV4 xác nhận |
|---|---|---|
| `StateStore.load()` / `save(ApplicationState)` `throws IOException` | **[CÓ]** + `MemoryStateStore` | Giữ nguyên; `FileStateStore` còn là file khung |
| `ApplicationState` (`users/items/auctions/bids` map) | **[CÓ]** | Hiện **không `Serializable`**; `bids` lặp dữ liệu với `Auction.history` — nguồn sự thật là map nào? |
| `PasswordHasher.hash(String)` / `verify(String, String)` | **[CÓ]** (đối tượng, không static) | Ai gọi (UserService); `LoginPayload.password` là plaintext trên đường truyền — chấp nhận ở mức bài tập? |

---

## 4. Ánh xạ nội bộ → DTO (TV3 viết mapper)

| Nguồn | DTO | Ghi chú đổi tên / mất dữ liệu |
|---|---|---|
| `AuctionSnapshot` | `AuctionSummary` | `start`→`startTime`, `end`→`endTime`; bỏ `startingPrice`, `highestBidderId`, `winnerId`, `bids` |
| `AuctionSnapshot` | `AuctionDetail` | như trên; `winner()` (Optional) → `winnerId` nullable; `bids: List<Bid>` → `history: List<BidSummary>` |
| `Bid` | `BidSummary` | `placedAt`→`acceptedAt` |
| `AuctionSnapshot.highestBidderId` | *(không có field)* | client lấy `bidderId` của phần tử cuối `history`; `AuctionSummary` không hiển thị được người dẫn đầu |
| `User` | `UserSummary(id, username, role)` | không bao giờ có hash |
| `Item` | `ItemSummary`, `ItemDetail` | `ItemSummary` không có `description` |

---

## 5. Danh sách xác nhận

- [ ] **TV1**: chốt chữ ký `register/login/listUsers/setUserActive/*Item` (mục 3.1) và nơi lưu `active` + hash.
- [ ] **TV2**: chốt `placeBid`, `confirmPayment`, `cancelAuction`, Observer (mục 3.2); xác nhận đổi tên `start/end`→`startTime/endTime`, `placedAt`→`acceptedAt` chỉ ở tầng mapper.
- [ ] **TV4**: chốt `ApplicationState` Serializable, nguồn sự thật của `bids`, vị trí gọi `PasswordHasher` (mục 3.3).
- [ ] **Cả nhóm**: duyệt chữ ký [ĐỀ XUẤT] ở mục 2 và các quyết định mở ở [schema-v1.md](schema-v1.md#9-khoảng-trống-và-quyết-định-mở).