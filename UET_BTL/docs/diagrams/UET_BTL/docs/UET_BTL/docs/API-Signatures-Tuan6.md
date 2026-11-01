# Chữ ký API — TV3 Lê Hải Thịnh — Tuần 6

Nguồn: nhánh `main` @ `195e041` (đã gồm `thinht6`) · Package `vn.uet.auction.protocol` · Tất cả kiểu là `record`/`enum`, bất biến, `Serializable`, `serialVersionUID = 1L`.
Đã kiểm chứng: biên dịch được (`javac --release 17`), round-trip serialization đạt, tên field khớp code. Chưa chạy `ProtocolContractTest` bằng Maven.

## 1. Envelope

| Kiểu | Chữ ký | Ràng buộc |
|---|---|---|
| `Request` | `record Request(UUID requestId, Command command, Payload payload)` | cả 3 field non-null (NPE) |
| `Request.Payload` | `sealed interface Payload extends Serializable permits LoginPayload, CreateItemPayload, CreateAuctionPayload, PlaceBidPayload` | chỉ 4 loại |
| `Response<T>` | `record Response<T extends Serializable>(UUID requestId, boolean success, ErrorCode errorCode, String message, T data)` | thành công: `errorCode == null`, `data` non-null · thất bại: `errorCode` non-null, `message` không trắng, `data == null` |
| `Response.ok` | `static <T extends Serializable> Response<T> ok(UUID requestId, T data)` | `message = ""` |
| `Response.fail` | `static <T extends Serializable> Response<T> fail(UUID requestId, ErrorCode errorCode, String message)` | `data = null` |
| `EventMessage` | `record EventMessage(EventType eventType, UUID auctionId, Instant occurredAt)` | cả 3 non-null |

## 2. Payload

| Payload | Chữ ký | Dùng cho Command | Ghi chú |
|---|---|---|---|
| `LoginPayload` | `(String username, String password)` | `LOGIN` | `toString()` che credentials |
| `CreateItemPayload` | `(ItemType type, String name, String description)` | `CREATE_ITEM` | |
| `CreateAuctionPayload` | `(UUID itemId, BigDecimal startingPrice, Instant startTime, Instant endTime)` | `CREATE_AUCTION` | |
| `PlaceBidPayload` | `(UUID auctionId, BigDecimal amount)` | `PLACE_BID` | |

Tất cả field payload non-null. Chưa kiểm tra nghiệp vụ (giá > 0, start < end) và chưa kiểm tra command khớp payload (dispatcher làm).

## 3. Enum

| Enum | Giá trị |
|---|---|
| `Command` (15) | `REGISTER, LOGIN, LIST_USERS, SET_USER_ACTIVE, CREATE_ITEM, LIST_ITEMS, GET_ITEM, UPDATE_ITEM, DELETE_ITEM, CREATE_AUCTION, LIST_AUCTIONS, GET_AUCTION, PLACE_BID, CONFIRM_PAYMENT, CANCEL_AUCTION` |
| `ErrorCode` (10) | `INVALID_REQUEST, UNSUPPORTED_COMMAND, AUTHENTICATION_FAILED, UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, CONFLICT, INVALID_BID, AUCTION_CLOSED, INTERNAL_ERROR` |
| `EventMessage.EventType` (4) | `BID_ACCEPTED, AUCTION_STARTED, AUCTION_FINISHED, AUCTION_CANCELED` |

## 4. DTO — `vn.uet.auction.protocol.dto`

| DTO | Chữ ký | Nguồn | Nhóm duyệt cùng |
|---|---|---|---|
| `UserSummary` | `(UUID id, String username, Role role)` | `User` (không hash) | TV1 |
| `ItemSummary` | `(UUID id, UUID sellerId, String name, ItemType type)` | `Item` | TV1 |
| `ItemDetail` | `(UUID id, UUID sellerId, String name, String description, ItemType type)` | `Item` | TV1 |
| `AuctionSummary` | `(UUID id, UUID itemId, UUID sellerId, BigDecimal currentPrice, AuctionStatus status, Instant startTime, Instant endTime)` | `AuctionSnapshot` | TV2 |
| `AuctionDetail` | `(UUID id, UUID itemId, UUID sellerId, BigDecimal startingPrice, BigDecimal currentPrice, AuctionStatus status, Instant startTime, Instant endTime, UUID winnerId, List<BidSummary> history)` | `AuctionSnapshot` | TV2 |
| `BidSummary` | `(UUID id, UUID auctionId, UUID bidderId, BigDecimal amount, Instant acceptedAt)` | `Bid` | TV2 |

Mọi field non-null, **trừ** `AuctionDetail.winnerId` (null = chưa có người thắng). `history` tăng dần theo giá, được copy và không sửa được.

## 5. Command → Payload → Data (đã có trong code)

| Command | Payload | `data` dự kiến | Payload trong code |
|---|---|---|---|
| `LOGIN` | `LoginPayload` | `UserSummary` | có |
| `CREATE_ITEM` | `CreateItemPayload` | `ItemDetail` | có |
| `CREATE_AUCTION` | `CreateAuctionPayload` | `AuctionDetail` | có |
| `PLACE_BID` | `PlaceBidPayload` | `BidSummary` | có |
| 11 command còn lại | — | — | **chưa có** (cần `RegisterPayload`, `EmptyPayload`, `SetUserActivePayload`, `ItemIdPayload`, `UpdateItemPayload`, `AuctionIdPayload`, và `Ack` cho kết quả rỗng) |

## 6. Phụ thuộc cần xác nhận

| Với | Chữ ký TV3 cần | Trạng thái |
|---|---|---|
| TV1 | `User`: `getId()`, `getUsername()`, `getRole()` · `Item`: `getId()`, `getSellerId()`, `getName()`, `getDescription()`, `getType()` | có |
| TV1 | `register`, `login`, `listUsers`, `setUserActive`, `create/list/get/update/deleteItem` | `UserService`/`ItemService` còn là file khung |
| TV2 | `createAuction(UUID, UUID, BigDecimal, Instant, Instant)`, `getAuction(UUID)`, `listAuctions()`, `closeDueAuctions()` → `AuctionSnapshot` / `StatusChange` | có |
| TV2 | `placeBid(UUID, UUID, BigDecimal)` | khai báo nhưng ném `UnsupportedOperationException` |
| TV2 | `confirmPayment`, `cancelAuction` | chưa có |
| TV4 | `StateStore.load()` / `save(ApplicationState)`, `PasswordHasher.hash/verify` | có |

## 7. Ánh xạ khi viết mapper

| Nguồn | Đích | Đổi tên |
|---|---|---|
| `AuctionSnapshot.start` / `end` | `startTime` / `endTime` | có |
| `Bid.placedAt` | `BidSummary.acceptedAt` | có |
| `AuctionSnapshot.winner()` (Optional) | `AuctionDetail.winnerId` (nullable) | có |
| `AuctionSnapshot.bids` (`List<Bid>`) | `history` (`List<BidSummary>`) | có |
