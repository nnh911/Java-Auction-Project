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

## TV3 tuần 7 — dispatcher User/Item (2026-11-05)

Phương án payload/kết quả/mapping được người dùng xác nhận đã duyệt trong chat.
**Đã triển khai**, chưa ghi nhận review chéo của TV1/TV4. Không coi xác nhận chat
là bằng chứng tất cả thành viên đã review code.

### Tích hợp bàn giao

Lấy nguyên UserService, ItemService, AccountState và ApplicationState từ local ref
origin/nnhoangt7 @ 3de0ab531439bf6cc8128c3da0868f31b47c637b.
UserService đã so sánh khớp nội dung file đính kèm. Đây là tích hợp code TV1,
không nhận là code nghiệp vụ TV3. ApplicationState thuộc TV4 có bổ sung accounts;
cần TV4 giữ map này và copy constructor khi ghép. Không thay đổi nghiệp vụ TV1.

### API và sử dụng

- `CommandDispatcher(UserService users, ItemService items, ReentrantLock lock)`;
  `Response<? extends Serializable> dispatch(Session session, Request request)`.
- Phải dùng cùng State/lock khi khởi tạo mọi service và dispatcher. Một Session
  cho một client, gọi tuần tự; Session không thread-safe. Dispatcher chưa có Socket.
- Session: constructor public không tham số; `boolean isAuthenticated()`,
  `UUID userId()`, `Role role()`, `void bind(UserSummary user)`, `void clear()`.
  userId/role ném IllegalStateException khi chưa login. bind chỉ dành cho code tin cậy.
- Request bổ sung nested payload: `EmptyPayload()`,
  `RegisterPayload(String username, String password, Role role)`,
  `ItemIdPayload(UUID itemId)`,
  `UpdateItemPayload(UUID itemId, ItemType type, String name, String description)`,
  `SetUserActivePayload(UUID userId, boolean active)`.
  Field tham chiếu non-null; constructor ném NPE trước dispatch nếu thiếu field.
  RegisterPayload.toString che credentials. Không có actor trong payload.
- `Ack()` implements Serializable; `ListResult<T extends Serializable>(List<T> values)`
  implements Serializable, accessor values(), copy list bất biến; null bị từ chối.

| Command | Payload | Response data |
|---|---|---|
| REGISTER | RegisterPayload | UserSummary |
| LOGIN | LoginPayload | UserSummary |
| LIST_USERS | EmptyPayload | ListResult<UserSummary> |
| SET_USER_ACTIVE | SetUserActivePayload | Ack |
| CREATE_ITEM | CreateItemPayload | UUID |
| LIST_ITEMS | EmptyPayload | ListResult<ItemSummary> |
| GET_ITEM | ItemIdPayload | ItemDetail |
| UPDATE_ITEM | UpdateItemPayload | Ack |
| DELETE_ITEM | ItemIdPayload | Ack |

```java
ApplicationState state = new ApplicationState();
ReentrantLock lock = new ReentrantLock();
UserService users = new UserService(state, lock, new PasswordHasher());
ItemService items = new ItemService(state, lock);
CommandDispatcher dispatcher = new CommandDispatcher(users, items, lock);
Session session = new Session();
var response = dispatcher.dispatch(session, new Request(UUID.randomUUID(),
        Command.LOGIN, new Request.LoginPayload("seller_demo", "password")));
```

Response giữ nguyên requestId. Login hợp lệ về payload xóa session cũ trước xác thực;
login thất bại không giữ danh tính cũ. REGISTER không tự login. Mọi command sau login
kiểm tra lại active, không tin role cache trong session. bootstrapAdmin không expose.
Payload sai → INVALID_REQUEST; chưa login → UNAUTHENTICATED; sai login →
AUTHENTICATION_FAILED; sai quyền/khóa → FORBIDDEN; get/mutation không tồn tại →
NOT_FOUND; IllegalStateException nghiệp vụ → CONFLICT; dữ liệu sai → INVALID_REQUEST;
lỗi RuntimeException khác → INTERNAL_ERROR. Response lỗi chỉ gửi tên mã, không gửi
exception/password/hash. Null Request/Session là lỗi caller, ném NPE vì không có
requestId hợp lệ để trả response. Kiểm tra tồn tại và mutation giữ cùng lock chung.
Auction commands chưa triển khai: sau kiểm tra đăng nhập/active trả UNSUPPORTED_COMMAND.
Không suy đoán Observer hoặc quyền Auction từ API tuần 6.

### Kiểm chứng

Đã biên dịch toàn bộ main + ba smoke test bằng javac --release 17;
DispatcherSmokeTest, ProtocolMapperSmokeTest, ClientStateSmokeTest đều PASS.
DispatcherSmokeTest cũng là console client dùng command với service thật:
register/login/CRUD/admin active, sai password, cấm register Admin, trùng username,
payload sai, chưa login, sai chủ, not found và session account bị khóa.
Chưa chạy Maven/JUnit, GUI, Socket, Auction/Observer; các smoke test dùng main(),
không tự chạy trong Surefire. Cần TV1 review quyền/error mapping; TV4 review State;
TV2 bàn giao Auction/event trước khi hoàn tất toàn bộ kịch bản W7.

## Đồng bộ bàn giao TV2 tuần 7 — 2026-11-05

Đã triển khai phần TV3 theo tv2-week7-contract.md do người dùng cung cấp:

- `client.network.AuctionGateway`: public interface, sáu chữ ký giữ nguyên hợp đồng:
  `CompletionStage<AuctionDetail> getAuction(UUID)`,
  `CompletionStage<AuctionDetail> placeBid(UUID, BigDecimal)`,
  `CompletionStage<AuctionDetail> confirmPayment(UUID)`,
  `CompletionStage<AuctionDetail> cancelAuction(UUID)`,
  `void addEventListener(ClientEventListener)`, `void removeEventListener(ClientEventListener)`.
  Đây là interface đã triển khai, chưa có adapter/transport thực thi. Các yêu cầu
  non-null, bất đồng bộ và listener phải được adapter tuân thủ khi có code thật.
- `ClientEventListener`: `void onEvent(EventMessage event)`; thay thế đề xuất cũ
  có onConnectionLost trong schema-v1.md. Không sửa schema lịch sử để che khác biệt.
- `GatewayException(ErrorCode errorCode, String message)` và
  `ErrorCode getErrorCode()`: null bị NPE, message trắng bị IAE.
- `Request.AuctionIdPayload(UUID auctionId)`: UUID non-null, thêm sealed permits;
  dùng GET_AUCTION/CONFIRM_PAYMENT/CANCEL_AUCTION.
- `EventMessage.EventType.PAYMENT_CONFIRMED` đã thêm; constructor giữ nguyên.
- Dispatcher kiểm tra payload cho cả sáu command Auction; chưa dispatch nghiệp vụ
  Auction vì code TV2 trong checkout/local origin/cngt7 còn tuần 6. Đúng payload
  vẫn trả UNSUPPORTED_COMMAND sau kiểm tra session/active. Không giả lập thành công.

Hợp đồng đã chốt, CHƯA triển khai do thiếu code TV2:
GET/CREATE/PLACE_BID/CONFIRM_PAYMENT/CANCEL_AUCTION trả AuctionDetail;
LIST_AUCTIONS trả ArrayList<AuctionSummary>, khác ListResult dùng cho User/Item
(các command User/Item giữ nguyên). Không đổi kết quả pay/cancel thành Ack.
Mapper event nội bộ chờ AuctionEvent thật; không tạo class giả trong src/main.
Khi ghép Auction, bắt buộc tách khỏi lock bao ngoài của nhánh User/Item trước khi
call public AuctionService; nếu không callback sau unlock của service vẫn nằm
trong lock dispatcher. Hiện không có lời gọi Auction nào nên chưa phát sinh lỗi đó.

Còn cần: code TV2 AuctionService/Manager/Observer, phân loại exception;
TV1 triển khai getUser(UUID), isUserActive(UUID), getItem(UUID) theo hợp đồng TV2;
TV2 nối quyền trong lock nghiệp vụ và TV4 nối State/lock. Không tự sửa module khác.

Đã chạy: main + bốn smoke test biên dịch release 17; DispatcherSmokeTest,
GatewayContractSmokeTest, ProtocolMapperSmokeTest, ClientStateSmokeTest PASS.
GatewayContractSmokeTest kiểm tra AuctionIdPayload serialization, validation
GatewayException và enum mới. ProtocolContractTest đã bổ sung event mới nhưng
chưa chạy JUnit/Maven trong lượt này. Chưa kiểm chứng adapter async/event/FX.


## Tích hợp Auction tuần 7 — 08/11/2026

Phần này cập nhật trạng thái Auction trong các mục trước. Đã tích hợp backend và test
Auction từ TV2, origin/cngt7 @ 275fbed, vào working tree hthinht7.

`CommandDispatcher(UserService, ItemService, ReentrantLock, AuctionService)`
xử lý sáu command Auction. Constructor ba tham số vẫn dùng cho User/Item.
Khởi tạo AuctionService bằng cùng ApplicationState, lock, UserService và ItemService.
Dispatcher lấy actor từ Session và gọi AuctionService ngoài lock bao ngoài.
`ProtocolMapper.toEventMessage(AuctionEvent)` giữ loại event, ID và thời điểm,
chỉ chuyển dữ liệu cần để client refresh.

Chữ ký service hiện có được giữ nguyên. Bổ sung phân loại lỗi có kiểu trong
AuctionService và kiểm tra active cho pay/cancel dưới lock để dispatcher trả lỗi
mà không đọc message. TV2 cần review hai thay đổi này. Cancel hiện theo quyền chủ
phiên của bản TV2. Chỉ mục State.bids và adapter gateway nằm ngoài luồng này.

Maven test offline đạt 86 test. Bốn smoke test chạy riêng đều PASS.
DispatcherSmokeTest chạy bằng main và kiểm tra callback sau unlock, gồm luồng
đăng nhập, CRUD, tạo phiên, bid, lấy chi tiết và hủy. Maven clean test offline
dừng ở plugin clean vì cache thiếu dependency. Chưa có nhánh tuần-7-chung trên
origin ở thời điểm kiểm tra.
