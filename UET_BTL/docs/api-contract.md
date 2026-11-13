# Hợp đồng API — baseline sau tuần 6


Cập nhật 2026-10-31 từ code nhánh tuần-6-chung (revision trước cập nhật tài liệu: 148da45). Đây là hợp đồng triển khai hiện tại để các thành viên/AI tiếp tục, không tuyên bố cả bốn người đã review hay mọi quy tắc nghiệp vụ đã được duyệt.

Đọc [bàn giao tuần 6](week6-handoff.md) trước khi triển khai tuần 7. Khi đổi chữ ký, cập nhật caller, test và tài liệu trong cùng thay đổi.

## Kiểu chung và trách nhiệm

- Package gốc vn.uet.auction; ID dùng UUID, tiền BigDecimal, thời gian Instant/Clock.
- TV1: model.user/model.item/factory và UserService/ItemService; TV2: model.auction/AuctionManager/AuctionService; TV3: protocol/DTO/mapping/client-server; TV4: persistence/security/pom.
- Tránh đưa Socket/JavaFX vào domain. Không trả password/hash qua DTO. Actor trên mạng phải lấy từ session server sau xác thực; session thật chưa có.
- Model User/Item bất biến, Serializable; Auction mutable và Serializable; Bid bất biến và Serializable. serialVersionUID đã khai báo. ApplicationState hiện chưa Serializable.

## TV1 — model và Factory đã có

| Kiểu | API |
|---|---|
| Role | BIDDER, SELLER, ADMIN |
| User abstract | protected User(UUID id, String username); getId(), getUsername(), abstract getRole() |
| Bidder/Seller/Admin | public constructor cùng tham số User; getRole theo subtype |
| ItemType | ELECTRONICS, ART, VEHICLE |
| Item abstract | protected Item(UUID id, UUID sellerId, String name, String description); getId(), getSellerId(), getName(), getDescription(), abstract getType() |
| Electronics/Art/Vehicle | public constructor cùng tham số Item; getType theo subtype |
| ItemCreator abstract | Item createItem(UUID id, UUID sellerId, String name, String description) |
| ElectronicsCreator/ArtCreator/VehicleCreator | override createItem, trả subtype tương ứng |

Tham số model bắt buộc không null (NullPointerException). Username strip rồi khớp [A-Za-z0-9_]{3,30}. Tên Item strip, dài 1–120; description strip, dài 0–2000. Sai giá trị: IllegalArgumentException. Caller cấp ID.

sellerId chưa chứng minh user là Seller. Chưa có password/active trong User; chưa triển khai register/login/CRUD service thật. Cần TV1+TV4 chốt lưu hash và active trước khi thêm.

## TV2 — model/service hiện tại

Auction(UUID id, UUID itemId, UUID sellerId, BigDecimal startingPrice, Instant start, Instant end):
ID/giá/thời gian không null, giá > 0, start < end. Model mới OPEN, currentPrice = startingPrice. Auction(Auction other) sao chép dữ liệu và history để persistence không dùng chung Auction mutable.

getHistory trả bản sao không sửa được. getHighestBid trả Optional<Bid>; getWinnerId trả Optional<UUID>, chỉ có winner ở FINISHED/PAID và có bid. refreshStatus, acceptBid, markPaid, cancel là public để service/test khác package gọi; production phải đi qua service kiểm tra quyền và giữ lock. Public không đồng nghĩa caller có quyền nghiệp vụ.

| AuctionService | Đầu ra / hành vi |
|---|---|
| AuctionService(Clock clock) | State/lock riêng để test/độc lập |
| AuctionService(Clock clock, ReentrantLock lock, Map<UUID,Auction> auctions) | Inject dữ liệu/lock chung; không tự copy map |
| createAuction(UUID sellerId, UUID itemId, BigDecimal startingPrice, Instant start, Instant end) | AuctionSnapshot; end phải sau clock.instant(); không trùng item với phiên chưa CANCELED; sinh UUID, refresh trạng thái trước trả |
| getAuction(UUID auctionId) | Optional<AuctionSnapshot>; không tìm thấy trả empty |
| listAuctions() | List<AuctionSnapshot> không sửa được |
| closeDueAuctions() | List<StatusChange>; tick OPEN/RUNNING theo Clock, không đổi PAID/CANCELED |
| placeBid(UUID auctionId, UUID bidderId, BigDecimal amount) | Đã triển khai lõi tuần 7; trả AuctionSnapshot sau khi cập nhật giá/history trong cùng lock. Chưa xác thực role/active; Observer đã triển khai, xem bàn giao tiếp tục bên dưới |

createAuction chưa kiểm tra role/ownership Item. Hiện lỗi dùng NullPointerException/IllegalArgumentException/IllegalStateException; chưa có mapping exception nghiệp vụ hoàn chỉnh. get/list không tự tick nên trạng thái có thể cũ tới lần closeDueAuctions.

AuctionManager.getInstance dùng holder Singleton, facade gọi service. Manager hiện tự tạo AuctionService(Clock.systemUTC()) với map/lock riêng, CHƯA nối State của TV4. Test nghiệp vụ tạo service riêng; việc wiring State/lock chung cần hoàn thiện trước tích hợp production.

AuctionSnapshot record gồm id,itemId,sellerId,startingPrice,currentPrice,start,end,status,highestBidderId,winnerId,bids. Hai bidder/winner ID có thể null. bids là List<Bid> được copy; winner() trả Optional và bidCount() trả int. Snapshot là nội bộ, không Serializable và không dùng trực tiếp làm Response.data.

## TV3 — protocol v1, bàn giao tuần 8

**Đã triển khai.** Bảng dưới đây mô tả dispatcher tại bản tích hợp tuần 7 và cập nhật
kiểm tra payload/session và ánh xạ ngoại lệ tuần 8. Chưa ghi nhận nhóm duyệt đóng băng schema.
Mục TV3 ở baseline tuần 6 và các ghi chú lịch sử về dispatcher trong phần TV2
phản ánh checkout cũ. Protocol v1 là tên schema tài liệu, chưa có trường version.

`Request(UUID requestId, Command command, Payload payload)` và các record payload
yêu cầu mọi trường tham chiếu có giá trị. Constructor từ chối null bằng
`NullPointerException`. Dispatcher kiểm tra cặp command/payload trước khi gọi service.
`LoginPayload` và `RegisterPayload` che credentials trong `toString()`.

| Command | Record payload thuộc `Request` | Kiểu `Response.data()` khi thành công |
|---|---|---|
| REGISTER | RegisterPayload(String username, String password, Role role) | UserSummary |
| LOGIN | LoginPayload(String username, String password) | UserSummary |
| LIST_USERS | EmptyPayload() | ListResult<UserSummary> |
| SET_USER_ACTIVE | SetUserActivePayload(UUID userId, boolean active) | Ack |
| CREATE_ITEM | CreateItemPayload(ItemType type, String name, String description) | UUID |
| LIST_ITEMS | EmptyPayload() | ListResult<ItemSummary> |
| GET_ITEM | ItemIdPayload(UUID itemId) | ItemDetail |
| UPDATE_ITEM | UpdateItemPayload(UUID itemId, ItemType type, String name, String description) | Ack |
| DELETE_ITEM | ItemIdPayload(UUID itemId) | Ack |
| CREATE_AUCTION | CreateAuctionPayload(UUID itemId, BigDecimal startingPrice, Instant startTime, Instant endTime) | AuctionDetail |
| LIST_AUCTIONS | EmptyPayload() | ArrayList<AuctionSummary> |
| GET_AUCTION | AuctionIdPayload(UUID auctionId) | AuctionDetail |
| PLACE_BID | PlaceBidPayload(UUID auctionId, BigDecimal amount) | AuctionDetail |
| CONFIRM_PAYMENT | AuctionIdPayload(UUID auctionId) | AuctionDetail |
| CANCEL_AUCTION | AuctionIdPayload(UUID auctionId) | AuctionDetail |

`Response<T extends Serializable>(UUID requestId, boolean success, ErrorCode errorCode,
String message, T data)` trả đúng ID của request. Thành công có data, errorCode null
và message rỗng. Lỗi có data null và message bằng tên ErrorCode khi đi qua dispatcher.

| Trường hợp | ErrorCode |
|---|---|
| Sai kiểu payload hoặc service từ chối tham số bằng IllegalArgumentException thông thường | INVALID_REQUEST |
| LOGIN bị từ chối bằng AuthenticationException | AUTHENTICATION_FAILED |
| Command cần đăng nhập nhưng session trống | UNAUTHENTICATED |
| Sai quyền, tài khoản bị khóa trong thao tác cần đăng nhập hoặc Rejection.FORBIDDEN | FORBIDDEN |
| Không tìm thấy mục tiêu được kiểm tra trong dispatcher hoặc Rejection.NOT_FOUND | NOT_FOUND |
| InvalidBidException qua Rejection.Reason.INVALID_BID | INVALID_BID |
| AuctionClosedException | AUCTION_CLOSED |
| IllegalStateException khác hoặc Rejection.CONFLICT | CONFLICT |
| Dispatcher được khởi tạo thiếu AuctionService khi gọi command đấu giá bằng session hợp lệ | UNSUPPORTED_COMMAND |
| RuntimeException khác, gồm NullPointerException nội bộ | INTERNAL_ERROR |

Mỗi client dùng một `Session` do server/test tạo và gửi request tuần tự. Mọi request
LOGIN xóa danh tính cũ trước khi xác thực, kể cả sai kiểu payload. REGISTER không
đăng nhập. Actor lấy từ session và tài khoản active được kiểm tra lại khi thao tác.

```java
var dispatcher = new CommandDispatcher(users, items, lock, auctions);
var session = new Session();
var login = dispatcher.dispatch(session, new Request(UUID.randomUUID(), Command.LOGIN,
        new Request.LoginPayload(username, password)));
if (login.success()) {
    var itemsResponse = dispatcher.dispatch(session, new Request(UUID.randomUUID(),
            Command.LIST_ITEMS, new Request.EmptyPayload()));
}
```

Các service tích hợp dùng chung state và lock. Dispatcher để AuctionService tự
quản lý lock để callback Observer chạy sau unlock. `ProtocolMapper` chuyển
AuctionSnapshot thành AuctionSummary/AuctionDetail và chuyển history thành
List<BidSummary>. winnerId có thể null. DTO phản hồi dùng giá trị và enum,
không chứa entity hoặc thông tin xác thực.

`EventMessage(EventType eventType, UUID auctionId, Instant occurredAt)` hỗ trợ
BID_ACCEPTED, AUCTION_STARTED, AUCTION_FINISHED, AUCTION_CANCELED và PAYMENT_CONFIRMED.
Kiểm tra ObjectStream tuần 8 dùng byte array trong test. Command null bị từ chối
khi dựng Request. Tên enum không nhận diện được trên stream thuộc tầng đọc dữ liệu
tuần 9, trước khi dispatcher có Request để giữ requestId.

**Đã triển khai theo bàn giao TV1/TV2.** Giữ nguyên chữ ký
`Response<? extends Serializable> dispatch(Session session, Request request)`.
Ngoại lệ trong `vn.uet.auction.exception` đều unchecked, với constructor public
nhận `String message`.

| Chủ file và commit | Ngoại lệ | Lớp cha trực tiếp |
|---|---|---|
| TV1, `0244c15` | AuthenticationException | SecurityException |
| TV2, `dc69b96` | InvalidBidException | AuctionService.Rejection, reason INVALID_BID |
| TV2, `dc69b96` | AuctionClosedException | IllegalStateException |

LOGIN trả AUTHENTICATION_FAILED cho credentials sai, trắng, user vắng mặt, thiếu
account hoặc account khóa. Request cần đăng nhập từ session đã bị khóa trả
FORBIDDEN. SET_USER_ACTIVE với userId vắng mặt trả NOT_FOUND. Nếu có User nhưng
thiếu AccountState, service hiện trả IllegalArgumentException và dispatcher giữ
INVALID_REQUEST. Cần TV1 chốt nếu muốn thống nhất hai trường hợp thiếu dữ liệu.

Trong service tích hợp, thứ tự xử lý PLACE_BID là kiểm tra tham số, role/active,
tìm phiên, cập nhật theo Clock, chặn chủ phiên, kiểm tra phiên RUNNING rồi kiểm tra
giá tăng. Giá bằng giá hiện tại đúng lúc end trả AUCTION_CLOSED. Nếu phiên chuyển
FINISHED trong lần gọi đó, Observer vẫn phát AUCTION_FINISHED sau unlock, còn giá
và lịch sử bid giữ nguyên.

**Chờ nhóm duyệt schema v1.** Bảng giữ kiểu danh sách hiện hành. Cần nhóm xác nhận
giữ các kiểu đó hay thống nhất qua một thay đổi có cập nhật caller. Kết quả build
TV4 tại `9f3c12b` thuộc nhánh TV4. Việc ghép TV4 cần phối hợp với TV2 vì cả hai
nhánh sửa AuctionService và test đấu giá, gồm cập nhật chỉ mục bid của TV4.

**Kiểm chứng TV3 ngày 2026-11-09.** `mvn -f UET_BTL/pom.xml clean test`
trên `hthinht8` tại merge `daf3791` cùng thay đổi mapping/test TV3, dùng Maven
3.9.16 và JDK 25.0.4.1, compile release 17. Kết quả BUILD SUCCESS, 111 test,
0 failure, 0 error, 0 skipped. Chưa đo JaCoCo cho bản ghép này.

## TV4 — StateStore và password

ApplicationState có getUsers/getItems/getAuctions/getBids trả Map<UUID,...> mutable. Constructor rỗng tạo maps mới. Copy constructor:
- copy map users/items/bids, dùng chung value hiện bất biến;
- tạo Auction mới qua copy constructor cho mỗi phiên.
Nếu thêm field mutable vào các model này phải xem lại chiến lược copy.

StateStore: ApplicationState load() throws IOException; void save(ApplicationState state) throws IOException.
MemoryStateStore ban đầu rỗng; save(null) ném IllegalArgumentException; save/load tạo ApplicationState copy. Chưa thread-safe, không lưu file và chưa có test independence. StateStore vẫn khai báo IOException dù bản memory không thực hiện I/O.

History trong Auction và map bids hiện là hai nơi chứa bid; chưa có cơ chế đồng bộ. Cần TV2+TV4 thống nhất nguồn dữ liệu chuẩn trước khi placeBid thật.

PasswordHasher(): 600000 iterations; PasswordHasher(int): nhận 100000–1000000.
hash(String password): trả chuỗi PBKDF2WithHmacSHA256 có salt ngẫu nhiên; null/rỗng bị IllegalArgumentException.
verify(String password,String stored): boolean; sai mật khẩu/hash sai định dạng/null trả false. Chưa gắn vào UserService/login thật.

## Quy tắc tiếp theo và điểm chưa duyệt

Đề xuất kế hoạch: không cho public register ADMIN; không tự bid; winner xác nhận thanh toán mô phỏng; owner/Admin hủy phiên chưa PAID; chặn sửa/xóa Item đã có phiên. Chưa coi là đã triển khai hoặc đã cả nhóm xác nhận.

Nhóm cần ghi quyết định thật cho actor/role/active, password storage, đấu giá lại, giới hạn tiền, wiring State/lock, bids map/history, command payload/result/error mapping và save failure. Không suy ra các quyết định này chỉ từ tên API.

## TV2 — bàn giao tuần 7, 2026-11-02

**Đã triển khai, chưa ghi nhận nhóm duyệt:** giữ nguyên chữ ký placeBid(UUID auctionId,
UUID bidderId, BigDecimal amount), trả AuctionSnapshot. AuctionManager ủy quyền như cũ.
Ví dụ: service.placeBid(auction.id(), authenticatedBidderId, new BigDecimal("1100000")).
Caller phải xác thực ID/role/active; lõi hiện chỉ chặn bidderId trùng sellerId.
Không dùng service hiện tại làm ranh giới phân quyền production.

Mọi tham số bắt buộc non-null (NullPointerException). Trong lock: tìm phiên, đọc Clock
một lần, refresh trạng thái, chặn tự bid, kiểm tra RUNNING và amount > currentPrice,
tạo Bid UUID và cập nhật history/giá. Không tìm thấy, tự bid, giá không tăng:
IllegalArgumentException; phiên không RUNNING: IllegalStateException. Lỗi giá không
thêm history; bid tại end bị từ chối nhưng trạng thái vẫn FINISHED. Tiền giữ quy tắc
BigDecimal hiện có; chưa áp giới hạn/scale chưa được duyệt. Custom exception để tuần 8.
History trong Auction là nơi lõi này ghi bid; chưa ghi ApplicationState.getBids().

**Đề xuất tại bàn giao đầu tiên (trạng thái mới nhất xem mục tiếp tục bên dưới):**

- AuctionEvent(UUID auctionId, AuctionStatus from, AuctionStatus to, Instant occurredAt,
  AuctionSnapshot snapshot, Type type); Type gồm BID_ACCEPTED, AUCTION_STARTED,
  AUCTION_FINISHED, AUCTION_CANCELED, PAYMENT_CONFIRMED. Cần TV3 duyệt mapping
  vì protocol hiện không có PAYMENT_CONFIRMED.
- AuctionListener: void onAuctionEvent(AuctionEvent event).
- AuctionService/AuctionManager: void addListener(AuctionListener listener),
  void removeListener(AuctionListener listener). Đề xuất null bị từ chối, đăng ký trùng
  không nhân callback; phát sau unlock, cô lập RuntimeException từng listener.
  Bid lỗi không phát BID_ACCEPTED; refresh đóng phiên vẫn phát FINISHED dù bid bị từ chối.
  Cần chốt thứ tự event giữa nhiều thread, reentrancy và cách báo lỗi listener.
- AuctionSnapshot confirmPayment(UUID actorId, UUID auctionId);
  AuctionSnapshot cancelAuction(UUID actorId, UUID auctionId). Cần duyệt winner trả tiền,
  owner/Admin hủy OPEN/RUNNING/FINISHED, cấm hủy PAID; xác thực role/active từ nguồn
  server tin cậy. Chưa thêm API khi chưa có quyết định này.
- Scheduler chủ động: List<StatusChange> tick(), ủy quyền closeDueAuctions(); chưa chạy
  thread tự động tuần 7. Cần duyệt có cần interface riêng hay dùng service hiện có.
- TV1 cần chốt API lấy user/active và Item/owner, cùng lock dùng chung để tránh TOCTOU.
  Không thêm fake vào src/main. TV4 cần chốt history hay State.bids làm nguồn chuẩn.
- GUI: cần TV3 chốt gateway bất đồng bộ, dữ liệu mẫu và TV4 cung cấp JavaFX trong pom;
  fx:id/handler là API chung chưa chốt nên chưa sửa auction.fxml/controller.

**Mâu thuẫn còn mở:** file-map nói ngoài TV1 đều TODO nhưng TV2 đã có code; liên kết
week6-handoff.md không tồn tại. Javadoc placeBid cũ nói hợp đồng đã chốt và yêu cầu
custom exceptions tuần 7, trong khi kế hoạch xếp exceptions tuần 8 và api-contract
chưa chốt Observer/role; đã thay Javadoc trong file TV2 để phản ánh giới hạn thật.
Chưa sửa tài liệu hay code thuộc TV1/TV3/TV4.

Kiểm chứng đã chạy: `mvn -f UET_BTL/pom.xml compile test
-Dtest=AuctionTest,AuctionServiceTest,ConcurrentBiddingTest`: BUILD SUCCESS,
28 tests, 0 failures/errors/skipped. Máy chạy JDK 25, javac release 17 theo pom;
chưa chạy trực tiếp trên runtime JDK 17. Chưa chạy toàn bộ suite, GUI, Socket,
Observer hoặc integration payment/cancel service. Test model có kiểm tra markPaid/cancel,
không đồng nghĩa đã có phân quyền pay/cancel ở service. Không đo coverage.

## TV2 — tiếp tục W7 sau xác nhận của người dùng

**Đã triển khai:** người dùng yêu cầu “Tiếp tục triển khai” sau khi nhận chữ ký đề xuất.
Đây là xác nhận trong phiên làm việc, chưa là bằng chứng cả bốn thành viên đã duyệt.
Không đổi các chữ ký public sẵn có; thêm API TV2 đã nêu ở trên:

| Kiểu | API mới đã triển khai |
|---|---|
| AuctionEvent | record(UUID auctionId, AuctionStatus from, AuctionStatus to, Instant occurredAt, AuctionSnapshot snapshot, Type type) |
| AuctionEvent.Type | BID_ACCEPTED, AUCTION_STARTED, AUCTION_FINISHED, AUCTION_CANCELED, PAYMENT_CONFIRMED |
| AuctionListener | void onAuctionEvent(AuctionEvent event) |
| AuctionService và AuctionManager | void addListener(AuctionListener listener), void removeListener(AuctionListener listener) |
| AuctionService và AuctionManager | AuctionSnapshot confirmPayment(UUID actorId, UUID auctionId) |
| AuctionService và AuctionManager | AuctionSnapshot cancelAuction(UUID actorId, UUID auctionId) |
| AuctionScheduler | interface: List<StatusChange> tick() |

Cách sử dụng (service là AuctionService có Clock và State riêng hoặc được inject):

```java
AuctionListener listener = event -> System.out.println(event.type());
service.addListener(listener);
AuctionScheduler scheduler = service::closeDueAuctions;
scheduler.tick();
service.placeBid(auctionId, authenticatedBidderId, new BigDecimal("1100000"));
// Khi Clock đã tới end:
scheduler.tick();
service.confirmPayment(authenticatedWinnerId, auctionId);
// Nhánh phiên chưa PAID:
// service.cancelAuction(authenticatedOwnerId, anotherAuctionId);
service.removeListener(listener);
```

Tất cả đối số mới bắt buộc non-null (NPE). Event kiểm tra ID/status khớp snapshot
(IAE nếu sai). Đăng ký listener trùng theo equals chỉ một lần; remove vắng mặt là no-op.
Snapshot event chụp ngay khi cập nhật, không trả Auction mutable.
Callback đồng bộ sau unlock; RuntimeException từng listener được log và cô lập.
Không nuốt Error của JVM. Callback nên nhanh, không block/chạy Socket trực tiếp.
Listener có thể đọc service, tự gỡ, hoặc gọi service lại sau unlock; thay đổi từ callback
có thể xen vào đợt phát hiện tại. Không cam kết thứ tự callback toàn cục giữa các thread
hay giữa thao tác tái nhập; consumer nên dùng snapshot trong event hoặc query lại.
Không gọi public service khi caller đang giữ lock inject từ bên ngoài: callback cần lock
được nhả hoàn toàn. Quy ước này phải được giữ khi TV4 nối snapshot/transaction.

createAuction đang RUNNING phát AUCTION_STARTED; tạo OPEN không phát event.
Tick bỏ qua nhiều mốc phát một AUCTION_FINISHED từ OPEN (giữ hành vi StatusChange cũ).
Bid thành công phát BID_ACCEPTED, from/to đều RUNNING; tick/refresh phát sự kiện
chuyển trạng thái trước event bid/payment. Bid lỗi không phát BID_ACCEPTED nhưng
refresh hợp lệ trước lỗi vẫn phát STARTED/FINISHED và giữ trạng thái mới.

confirmPayment: tự refresh theo Clock, chỉ ID winner được xác nhận FINISHED có bid;
markPaid chặn trả lần nữa. Không có winner/sai actor: IAE; trả lần nữa: ISE.
cancelAuction: chỉ owner ID; tự refresh rồi hủy OPEN/RUNNING/FINISHED,
PAID/CANCELED bị ISE; sai owner bị IAE. Unknown ID là IAE. Kiểm tra quyền không đủ
thay cho xác thực actor/role/active phía server. **Admin cancel chưa triển khai** vì
không có API trusted user lookup của TV1; không thêm tham số role do client tự khai.

Scheduler tuần 7 là interface và method reference, không khởi tạo background thread.
Để tuần 9 hoàn thiện lifecycle tự động theo kế hoạch. Không sửa enum protocol TV3;
PAYMENT_CONFIRMED hiện chỉ là event nội bộ, chờ TV3 duyệt mapping.

**Phụ thuộc còn thiếu:** API role/active/Item ownership từ TV1, lock và nguồn bid chuẩn
với TV4; gateway GUI từ TV3 và JavaFX dependency do TV4 sở hữu. AuctionController và
auction.fxml vẫn là khung, chưa được ghi nhận hoàn thành. Không sửa pom hoặc tạo giả
API module khác trong src/main. Chưa tuyên bố hoàn tất toàn bộ checklist W7.

Kiểm chứng mới nhất: `mvn -f UET_BTL/pom.xml test` BUILD SUCCESS, **80 tests**, không
failure/error/skipped; gồm 34 test đấu giá (6 lifecycle/Observer mới). Compile main/test
release 17 trên JDK 25. Log cảnh báo listener lỗi là tình huống kiểm thử chủ động.
`git diff --check` sạch. Chưa kiểm chứng runtime JDK 17, GUI/Socket/production State
hoặc đo coverage. Kết quả 28 tests phía trên là lượt kiểm tra trước khi bổ sung Observer.

## TV2 — tích hợp quyền tạo phiên và đặt giá

**Đã triển khai.** Backend User/Item và accounts lấy từ `origin/nnhoangt7`
tại `fe14aa0`. AuctionManager tạo một ApplicationState và ReentrantLock dùng chung.
Khi gọi qua Singleton, dùng `manager.getUserService()` và
`manager.getItemService()` để thao tác trên cùng dữ liệu với phiên đấu giá.

Constructor tích hợp là
`AuctionService(Clock, ReentrantLock, ApplicationState, UserService, ItemService)`.
Hai service được truyền vào phải dùng cùng state và lock. Constructor cũ dành cho
kiểm thử lõi độc lập. createAuction gọi requireOwnedItem trong lock trước khi
ghi phiên. placeBid gọi requireRole với BIDDER trong lock trước khi refresh.
Sai quyền hoặc account bị khóa ném SecurityException. Observer chạy sau unlock.

Dispatcher gọi service tích hợp hoặc manager trực tiếp, với actor từ session tin cậy.
CommandDispatcher trong checkout này vẫn là file khung. Đã xác minh qua service
và manager bằng Maven, 85 test pass.

## TV2 — hủy phiên và form nhập giá tuần 7

**Đã triển khai.** Với service tích hợp, `cancelAuction(actorId, auctionId)`
kiểm tra account active trong lock. Admin active hoặc Seller active sở hữu phiên
được hủy OPEN, RUNNING hoặc FINISHED. Sai quyền ném SecurityException.
Phiên PAID và CANCELED bị chặn theo quy tắc của Auction. Hủy thành công phát
AUCTION_CANCELED sau unlock. Chữ ký service và manager giữ nguyên.

Form `/view/auction.fxml` dùng `AuctionController`. Sau FXMLLoader.load, caller
gọi hai API sau trên FX thread:

```java
void configure(Supplier<CompletionStage<AuctionDetail>> refresh,
               Function<BigDecimal, CompletionStage<AuctionDetail>> placeBid)
void dispose()
```

Caller gắn phiên và danh tính vào callback. Ví dụ khi adapter gateway của TV3
đã sẵn sàng, truyền `() -> gateway.getAuction(auctionId)` và
`amount -> gateway.placeBid(auctionId, amount)`. Form tự tải khi configure,
cho làm mới thủ công và render kết quả trên FX thread. Callback phải trả stage
ngay. Gọi dispose khi rời màn hình. Code gateway và điều hướng còn chờ ghép.

Maven test đạt 86 test. Test đồng thời dùng 32 Bidder đăng ký thật với cùng
state/lock, xác nhận giá cuối 33, lịch sử tăng nghiêm ngặt và mỗi bid được nhận
có đúng một BID_ACCEPTED sau unlock. Đã kiểm tra tải FXML, cấu hình callback,
nhập giá và cập nhật hiển thị bằng JavaFX với dữ liệu thử.

## TV2 — quy tắc giá VND tuần 8

**Đã chốt và triển khai ngày 2026-11-09.** Giá khởi điểm và giá đặt phải là số
nguyên trong khoảng từ 1 đến 1.000.000.000.000 VND, gồm cả hai đầu. Service
nhận 1000.00 vì giá trị bằng 1000 và lưu dưới dạng BigDecimal có scale = 0.
Giá có phần lẻ thực sự bị từ chối. Bid phải lớn hơn giá hiện tại, kể cả bid
đầu tiên, nên mức tăng tối thiểu là 1 VND.

Giá khởi điểm sai ném IllegalArgumentException (INVALID_REQUEST). Trong
placeBid, thứ tự kiểm tra tham số, quyền, phiên tồn tại, chủ phiên và trạng
thái giữ nguyên. Với phiên RUNNING, giá có phần lẻ, ngoài khoảng hoặc không
tăng ném InvalidBidException (INVALID_BID). Phiên không RUNNING ném
AuctionClosedException (AUCTION_CLOSED) trước khi kiểm tra giá, kể cả tại
đúng end. Bid bị từ chối không đổi giá, không thêm history hay BID_ACCEPTED.
Refresh tại end vẫn có thể phát AUCTION_FINISHED.

## TV2 — scheduler tuần 9

**Đã triển khai.** `vn.uet.auction.scheduler.AuctionScheduler` vẫn là functional
interface với `List<StatusChange> tick()`. Lớp lồng `AuctionScheduler.Periodic`
triển khai interface này và `AutoCloseable`.

| API public | Hành vi |
|---|---|
| `Periodic(AuctionScheduler delegate, Duration interval)` | Nhận hàm tick và khoảng nghỉ dương biểu diễn được bằng nanosecond. Null gây `NullPointerException`, khoảng không dương gây `IllegalArgumentException`, tràn số gây `ArithmeticException`. |
| `void start()` | Tick ngay trên luồng nền, sau đó nghỉ theo interval tính từ lúc tick trước kết thúc. Gọi lại khi đang chạy có cùng hiệu lực. Sau stop, ném `IllegalStateException`. |
| `List<StatusChange> tick()` | Gọi delegate đồng bộ và trả kết quả trực tiếp. |
| `void stop()`, `void close()` | Hủy các lượt tự động tiếp theo. Lượt đang chạy có thể hoàn tất. Có thể gọi lặp. |

Lỗi `RuntimeException` từ tick nền được ghi log để các lượt sau tiếp tục.
`closeDueAuctions()` tiếp tục lấy giờ từ `Clock` của service và phát Observer sau khi nhả lock.

**Đề xuất tích hợp TV3.** Thành phần quản lý vòng đời server giữ một instance
`new AuctionScheduler.Periodic(auctionService::closeDueAuctions, Duration.ofSeconds(1))`,
gọi `start()` khi server sẵn sàng và `close()` khi shutdown. Nhóm cần thống nhất
thành phần sở hữu instance và chu kỳ mặc định.

**Đề xuất tích hợp TV1/TV4.** Chốt cách các service cùng đọc state đã commit
sau khi `StateStore.save(ApplicationState) throws IOException` thành công, cùng
lock với CRUD user/item. Cần thống nhất cách công bố snapshot mới, ánh xạ lỗi
lưu trữ và nơi gọi `StateStore.load() throws IOException` khi khởi động.
`AuctionManager` hiện khởi tạo state trong RAM, nên chưa có cam kết lưu bid qua restart.

### Bàn giao tuần 9 để gửi TV3

- Nối `CommandDispatcher(UserService, ItemService, ReentrantLock, AuctionService)` với một bộ
  service dùng chung `ApplicationState` và `ReentrantLock`. Scheduler nhận
  `auctionService::closeDueAuctions` từ chính bộ service này. Bản Socket hiện
  cần điểm khởi động/shutdown trong `ServerMain` và `AuctionServer`. Đề xuất
  một instance scheduler cho server, tick mỗi giây, đóng khi shutdown.
- `AuctionController.configure(Supplier<CompletionStage<AuctionDetail>>,
  Function<BigDecimal, CompletionStage<AuctionDetail>>)` đã có sẵn. Khi mở
  chi tiết phiên, bind hai callback vào ID của phiên đang xem. Callback đầu
  gọi `GET_AUCTION`, callback sau gửi `PLACE_BID` với ID và số tiền. Actor
  lấy từ session phía server. Thực hiện I/O ngoài FX thread, hoàn tất stage
  bằng `AuctionDetail` hoặc hoàn tất ngoại lệ khi response lỗi. Gọi
  `dispose()` khi bỏ controller. Cần SDK thật vì `AuctionClient` hiện là
  file khung. Các callback trên là API controller hiện có.
- Kiểm tra đường đi login → list → chi tiết → bid qua Socket. Thử bid hợp lệ,
  bid thấp và bid sau giờ đóng. Dispatcher hiện có `INVALID_BID` và
  `AUCTION_CLOSED` cho hai tình huống sau. Kiểm tra thêm phiên tự đóng khi
  client để yên, rồi truy vấn lại chi tiết để thấy trạng thái và người thắng.

### Đề xuất giao dịch chung để TV2, TV4 và TV1 chốt

1. Giữ hợp đồng `StateStore.load() throws IOException` và
   `StateStore.save(ApplicationState) throws IOException`. TV4 hiện thực
   `FileStateStore`; điểm khởi tạo gọi load một lần trước khi mở server.
   File thiếu cho state rỗng, file hỏng báo lỗi và dừng khởi động.
2. Đề xuất một lớp điều phối dùng chung, tên/chữ ký chờ duyệt:
   `vn.uet.auction.persistence.StateTransactions(StateStore store) throws IOException`,
   `<T> T read(Function<ApplicationState, T> operation)`,
   `<T> T write(Function<ApplicationState, T> operation, Predicate<T> changed) throws IOException`.
   Khi `changed` trả false, bỏ bản sao và trả kết quả, ví dụ tick có danh sách rỗng.
   Coordinator sở hữu lock và state đã commit. Callback làm việc trong lock
   và chỉ trả DTO/kết quả độc lập. Write sao chép state, áp dụng lệnh lên bản
   sao, gọi save, rồi công bố bản sao. Mutation user/item cũng đi qua write.
   Kiểm tra actor và quyền dùng state trong cùng giao dịch. TV1 cần duyệt
   việc chuyển các service đang giữ reference trực tiếp sang coordinator.
3. Chốt `Auction.history` là nguồn lịch sử chuẩn, `ApplicationState.bids` là
   chỉ mục. Bid hợp lệ cập nhật cả hai trong bản sao. Xác nhận copy constructor
   cô lập `Auction` và các kiểu dùng chung còn lại bất biến. Ghi lỗi thì giữ
   state cũ và bỏ event. Event chỉ phát sau save thành công và nhả lock.
4. Đề xuất lệnh bid bị từ chối không ghi snapshot. Nếu bid đến muộn, từ
   chối theo `Clock` ngay trong lock, để scheduler đóng và lưu phiên ở lượt
   kế tiếp. Đây là thay đổi cần duyệt vì hiện tại `placeBid` có thể chuyển
   FINISHED và phát event trước khi trả `AuctionClosedException`. Tick không
   đổi trạng thái nên bỏ qua save. Lỗi I/O đề xuất dùng `INTERNAL_ERROR`
   hiện có ở dispatcher, log nguyên nhân phía server và cho tick sau thử lại.
5. Tiêu chí bàn giao: tải lại được giá/history/winner sau restart; save lỗi
   giữ nguyên cả giá, lịch sử, index và trạng thái trong RAM, không phát event;
   bid đồng thời với tick không thêm bid muộn; CRUD thành công cũng được lưu.

Hiện `AuctionService` giữ map auction/bid trực tiếp, `UserService` và
`ItemService` giữ reference `ApplicationState`, còn `FileStateStore` là
file khung. Việc chỉ thay reference trong `AuctionManager` sẽ để các service
tiếp tục đọc reference cũ. Các chữ ký điều phối ở trên là đề xuất, chờ duyệt
trước khi sửa constructor hoặc nối lưu trữ.

### TV2 tuần 9 — giao dịch đấu giá đã triển khai

TV2 đã chốt phương án lưu trước khi công bố state. Phần tích hợp giữa các
thành viên vẫn chờ ghép nhánh. Các chữ ký sau đã có trong package
`vn.uet.auction.service`:

- `static AuctionManager open(StateStore store, Clock clock) throws IOException`
  tải snapshot trước khi tạo bộ service. Dùng một manager cho mỗi server.
  Đường gọi `getInstance()` vẫn dùng chế độ RAM.
- `AuctionService getAuctionService()` và `ReentrantLock getLock()` trên
  `AuctionManager` cung cấp đúng instance và lock để TV3 truyền vào
  `CommandDispatcher`. Scheduler dùng `manager::closeDueAuctions`.
- `AuctionService(Clock clock, StateTransactions transactions, UserService
  userService, ItemService itemService)` dùng coordinator chung.
- `StateTransactions(StateStore store) throws IOException`,
  `ReentrantLock lock()`, `<T> T read(Function<ApplicationState, T> operation)`
  và `<T> T write(Function<ApplicationState, T> operation, Predicate<T> changed)
  throws IOException`. Callback chạy dưới lock, chỉ trả kết quả độc lập và
  không giữ reference đến state hay gọi write lồng nhau.

Coordinator giữ identity của các map đang được service tham chiếu. Sau save,
nội dung các map được cập nhật dưới lock. Lớp nằm trong package service để
giữ phần điều phối trong phạm vi TV2. Các lệnh auction ghi chung snapshot;
tick không đổi trạng thái bỏ qua save. Bid đến muộn ở chế độ lưu trữ bị từ
chối và để scheduler đóng phiên. Lỗi ghi ném `UncheckedIOException`, được
nhánh `RuntimeException` hiện có của dispatcher ánh xạ sang `INTERNAL_ERROR`.
Observer chỉ nhận thay đổi sau khi save thành công và lock đã được nhả.

TV1 cần chuyển các mutation user/item sang coordinator. Hiện chúng dùng
chung state và lock nhưng chưa tự lưu khi đứng riêng. TV3 cần nối
`AuctionManager.open(new FileStateStore(path), Clock.systemUTC())` vào vòng đời
server sau khi ghép nhánh TV4. Không cần thay chữ ký command hay enum lỗi.
