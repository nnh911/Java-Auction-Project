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
| placeBid(UUID auctionId, UUID bidderId, BigDecimal amount) | Khai báo trả AuctionSnapshot nhưng hiện luôn ném UnsupportedOperationException; việc tuần 7 |

createAuction chưa kiểm tra role/ownership Item. Hiện lỗi dùng NullPointerException/IllegalArgumentException/IllegalStateException; chưa có mapping exception nghiệp vụ hoàn chỉnh. get/list không tự tick nên trạng thái có thể cũ tới lần closeDueAuctions.

AuctionManager.getInstance dùng holder Singleton, facade gọi service. Manager hiện tự tạo AuctionService(Clock.systemUTC()) với map/lock riêng, CHƯA nối State của TV4. Test nghiệp vụ tạo service riêng; việc wiring State/lock chung cần hoàn thiện trước tích hợp production.

AuctionSnapshot record gồm id,itemId,sellerId,startingPrice,currentPrice,start,end,status,highestBidderId,winnerId,bids. Hai bidder/winner ID có thể null. bids là List<Bid> được copy; winner() trả Optional và bidCount() trả int. Snapshot là nội bộ, không Serializable và không dùng trực tiếp làm Response.data.

## TV3 — protocol v1 theo code hiện tại

Đây là tên phiên bản tài liệu; code chưa có field version.

Request(UUID requestId, Command command, Payload payload): cả ba không null. sealed Payload hiện chỉ có:

| Payload | Fields |
|---|---|
| LoginPayload | String username, String password |
| CreateItemPayload | ItemType type, String name, String description |
| CreateAuctionPayload | UUID itemId, BigDecimal startingPrice, Instant startTime, Instant endTime |
| PlaceBidPayload | UUID auctionId, BigDecimal amount |

Payload kiểm tra non-null, chưa thay kiểm tra nghiệp vụ. Request chưa tự kiểm tra command có khớp loại payload; dispatcher/gateway phải kiểm tra. LoginPayload.toString che credentials.

Command có REGISTER, LOGIN, LIST_USERS, SET_USER_ACTIVE, CREATE_ITEM, LIST_ITEMS, GET_ITEM, UPDATE_ITEM, DELETE_ITEM, CREATE_AUCTION, LIST_AUCTIONS, GET_AUCTION, PLACE_BID, CONFIRM_PAYMENT, CANCEL_AUCTION.
Enum tồn tại không có nghĩa đã có payload/handler cho mọi command; không có empty payload hiện tại.

Response<T extends Serializable>(UUID requestId, boolean success, ErrorCode errorCode, String message, T data):
- ok(requestId,data): success true, errorCode null, message rỗng, data bắt buộc non-null.
- fail(requestId,errorCode,message): success false, data null, errorCode bắt buộc, message không trắng.
- Server/gateway phải trả đúng requestId; record không tự so với request.
- Chưa có hợp đồng success không data; cần thiết kế trước khi làm delete/cancel trả void.

ErrorCode: INVALID_REQUEST, UNSUPPORTED_COMMAND, AUTHENTICATION_FAILED, UNAUTHENTICATED, FORBIDDEN, NOT_FOUND, CONFLICT, INVALID_BID, AUCTION_CLOSED, INTERNAL_ERROR. Đây là enum đã có; error mapping thật chưa hoàn thiện.

EventMessage(EventType eventType, UUID auctionId, Instant occurredAt): tất cả non-null, Serializable. EventType: BID_ACCEPTED, AUCTION_STARTED, AUCTION_FINISHED, AUCTION_CANCELED. Chưa có transport phát event thật.

DTO Serializable: UserSummary, ItemSummary, ItemDetail, BidSummary, AuctionSummary, AuctionDetail. Đọc constructor record thật khi mapping. AuctionDetail gồm id,itemId,sellerId,startingPrice,currentPrice,status,startTime,endTime,winnerId,history; winnerId được null, history là List<BidSummary> được copy.

Mapping cần viết: snapshot.start/end → DTO.startTime/endTime; snapshot.bids → từng BidSummary trong history; winnerId giữ null nếu chưa có. Chưa có mapper production. Kiểu response cho từng command cần TV3 thống nhất với chủ service; không tự coi ví dụ fake là API đã duyệt.

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
