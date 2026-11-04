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

## Bổ sung TV3 tuần 7 — 2026-11-04

**Đã triển khai**, chưa ghi nhận nhóm duyệt: `vn.uet.auction.protocol.mapper.ProtocolMapper`.
Class final, constructor private; sáu phương thức public static:

```java
UserSummary toUserSummary(User user)
ItemSummary toItemSummary(Item item)
ItemDetail toItemDetail(Item item)
BidSummary toBidSummary(Bid bid)
AuctionSummary toAuctionSummary(AuctionSnapshot snapshot)
AuctionDetail toAuctionDetail(AuctionSnapshot snapshot)
```

Dùng: `AuctionDetail dto = ProtocolMapper.toAuctionDetail(snapshot);`.
Mapper chỉ đọc getter/snapshot hiện có; không gọi service, không kiểm tra quyền,
không chuyển trạng thái hay tự tạo bid. Mọi đầu vào null ném NullPointerException.
Giữ nguyên UUID, BigDecimal (kể cả scale), Instant, thứ tự history và winnerId nullable.
`placedAt` ánh xạ `acceptedAt`, `start/end` ánh xạ `startTime/endTime`.
DTO history bất biến, không chứa Auction sống hoặc thông tin xác thực.
Không thay đổi constructor, enum hay API của module khác.

Kiểm chứng đã chạy: javac --release 17 toàn bộ main và ProtocolMapperSmokeTest;
chạy main của smoke test PASS (mapping User/Item/Bid/Auction, winner null,
history bất biến, null input, serialization round-trip AuctionDetail).
Smoke test là chương trình console, KHÔNG phải JUnit và không tự chạy bởi Surefire.
Chưa chạy Maven/JUnit hoặc JavaFX; môi trường chưa có mvn, pom chưa khai báo JavaFX.

### Phần cần chốt trước khi tiếp tục — ĐỀ XUẤT, chưa triển khai

- `Response<? extends Serializable> dispatch(Session session, Request request)`;
  constructor nhận các service thật sau khi TV1/TV2 xác nhận chữ ký.
- Payload còn thiếu: EmptyPayload, RegisterPayload, ItemIdPayload,
  UpdateItemPayload, AuctionIdPayload, SetUserActivePayload. Fields/validation cần duyệt.
- Ack cho kết quả void và wrapper Serializable cho list; cần duyệt kiểu cụ thể.
- PLACE_BID trả BidSummary hay AuctionDetail: service hiện trả AuctionSnapshot,
  chưa triển khai bid; cần TV2 xác nhận kết quả chứa chính bid vừa chấp nhận.
- Cần API TV1 register/login/CRUD/active; API TV2 pay/cancel và Observer;
  cần TV4 cấu hình JavaFX trước prototype.

Mâu thuẫn đang giữ nguyên để phối hợp: schema-v1.md mục 2 xếp dispatcher
vào tuần 9–12, nhưng kế hoạch mục tuần 7 và file-map giao dispatcher từ tuần 7.
Kế hoạch giao prototype danh sách/chi tiết tuần 7, file-map ghi
ItemDetailController từ tuần 8. Cần thống nhất phạm vi màn chi tiết.
Không sửa tài liệu chung hoặc file của TV1/TV2/TV4 trong thay đổi này.

### ClientState — bổ sung tuần 7

**Đã triển khai trong phạm vi TV3; chưa ghi nhận nhóm duyệt API tích hợp.**
Package `vn.uet.auction.client.model`; `public final class ClientState`, constructor
public không tham số. Không phụ thuộc JavaFX/Socket/service, dùng trên một luồng UI.

```java
List<AuctionSummary> getAuctions()
void replaceAuctions(List<AuctionSummary> summaries)
Optional<UUID> getSelectedAuctionId()
Optional<AuctionDetail> getSelectedAuction()
void selectAuction(UUID auctionId)
boolean acceptSelectedAuction(AuctionDetail detail)
void clear()
```

Cách dùng: replaceAuctions(dtoList), selectAuction(id), acceptSelectedAuction(dto).
List đầu vào được copy bất biến; null list/phần tử bị từ chối bằng NPE.
ID trùng bị IllegalArgumentException và không đổi state. Chọn ID ngoài danh sách
bị IllegalArgumentException. Refresh giữ lựa chọn còn tồn tại và xóa detail cũ;
đổi lựa chọn xóa detail cũ. acceptSelectedAuction chỉ nhận detail khớp ID đang chọn,
trả false nếu khác; không đảm bảo thứ tự hai response cùng ID, caller cần quản lý
request/generation khi tích hợp mạng sau này. clear xóa danh sách/lựa chọn/detail.

Đã chạy lại javac --release 17 toàn bộ main + hai smoke test và chạy console:
ClientStateSmokeTest PASS, ProtocolMapperSmokeTest PASS. Chưa chạy Maven/JUnit/GUI.
ClientState là nền dữ liệu prototype, chưa phải màn hình JavaFX hoàn chỉnh.

### Trao đổi để hoàn thiện W7

| Người | TV3 cần nhận | TV3 cần gửi để phối hợp |
|---|---|---|
| TV1 | Branch/commit UserService, ItemService; chữ ký, return, exception cho login/register/CRUD/active; nơi kiểm tra quyền | DTO UserSummary/ItemSummary/ItemDetail hiện có; mapper chỉ đọc model; đề xuất payload/result cần duyệt trước code |
| TV2 | Branch/commit AuctionService, AuctionListener, AuctionEvent; kiểu actor pay/cancel; callback/event; exception; xác nhận placeBid snapshot | Mapper giữ nguyên start/end→startTime/endTime, placedAt→acceptedAt; đề xuất PLACE_BID trả AuctionDetail; không gọi model mutation trực tiếp |
| TV4 | Branch/commit pom JavaFX, ClientApp; cách chạy và version JDK thực tế; wiring State/lock chung với TV2 | Main/mapper/state biên dịch release 17; không sửa pom; hai smoke test console chưa thuộc Surefire |
| Cả nhóm | Duyệt payload cho command thiếu, Ack/list wrapper, dispatcher/session; thống nhất mốc prototype chi tiết | Dispatcher in-memory thuộc W7 theo kế hoạch; Socket thuộc giai đoạn sau; các API mới TV3 là đã triển khai, chưa coi là nhóm duyệt |
