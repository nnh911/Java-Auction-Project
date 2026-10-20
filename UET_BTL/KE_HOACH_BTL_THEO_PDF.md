# KẾ HOẠCH BÀI TẬP LỚN CHO NHÓM 4 NGƯỜI — THEO LỘ TRÌNH PDF

## 1. Mục tiêu, phạm vi và lịch thực hiện

Nhóm tự viết mã nguồn hệ thống đấu giá trực tuyến theo toàn bộ 7 trang **Hướng dẫn BTL LTNC-2026 (1).pdf** trong cùng thư mục. File này là kế hoạch để thực hiện, không phải báo cáo dự án đã hoàn thành. Workspace đã có khung thư mục và file TODO để nhóm tự viết nội dung; chưa có class, phương thức, logic nghiệp vụ hoặc kiểm thử được triển khai. Xem [README](README.md) và [danh mục file phụ trách](docs/file-map.md).

**Dùng đúng số tuần học kỳ trong PDF: tuần 6 đến tuần 15.** Backend, giao diện, mạng và dữ liệu được phát triển song song, tích hợp toàn bộ ở tuần 11–12, hoàn thiện ở tuần 13–14 và trình bày ở tuần 15. Không tự quy đổi thành ngày tháng khi chưa biết lịch lớp.

PDF nói có 9 tuần làm dự án từ tuần 6 trước buổi trình bày tuần 15. Kế hoạch vì vậy có chín tuần phát triển **6–14** và một tuần trình bày **15**. Tuần 6–10 còn bài giảng; từ tuần 11 không còn bài giảng. PDF gộp tuần 11–12 và 13–14; phần tách công việc riêng cho từng tuần bên dưới là đề xuất phân bổ của nhóm, giữ nguyên mục tiêu của hai giai đoạn trong đề.

| Tuần học kỳ | Nội dung theo PDF | Đầu ra cần hướng đến | Nguồn trong PDF |
|---|---|---|---|
| 6 | Khởi động, OOP, Singleton, Factory; bắt đầu JavaFX | Repo main/dev, class diagram, model cơ bản, JavaFX Hello World | Trang2 |
| 7 | Đa luồng, Observer, logic/vòng đời; GUI cơ bản | Bid an toàn trong bộ nhớ, thông báo nghiệp vụ, Login/Danh sách prototype | Trang3 |
| 8 | Ngoại lệ, JUnit, refactor SOLID; MVC/FXML | Ba custom exception, test logic đấu giá, controller tách business | Trang3 |
| 9 | Maven, Checkstyle, Actions; bắt đầu Socket và Serialization | Build/test tự động, luồng mạng và lưu/tải đầu tiên | Trang4 |
| 10 | Server nhiều client, realtime qua Socket; đủ màn hình chính; coverage≥60% | Client–Server hoạt động, GUI chính nối API, báo cáo coverage | Trang4 |
| 11 | Tích hợp toàn bộ, E2E, xử lý lỗi | GUI↔Logic↔Network↔Data chạy cùng nhau, danh sách lỗi tích hợp | Trang4–5, giai đoạn11–12 |
| 12 | Hoàn thiện tích hợp, Serialization, edge cases và CI xanh | Bản bắt buộc ổn định có bằng chứng E2E/restart/CI | Trang5, giai đoạn11–12 |
| 13 | Polish GUI/UX, kiểm thử; tùy chọn nếu kịp | Cải thiện sử dụng, quyết định có đủ điều kiện làm nâng cao | Trang5, giai đoạn13–14 |
| 14 | Hoàn thiện, regression, README; chốt tùy chọn đã test | Bản bàn giao và tài liệu cài/chạy, demo sẵn sàng | Trang5–6, giai đoạn13–14 |
| 15 | Trình bày, demo, đóng góp, vấn đáp | Server+nhiều Client, slide và minh chứng đóng góp thật | Trang5 |

### Yêu cầu bắt buộc và tùy chọn

- Java, JavaFX/FXML, Client–Server và MVC; không thay bằng website hoặc framework REST.
- User abstract→Bidder/Seller/Admin; Item abstract→Electronics/Art/Vehicle; Auction/Bid; áp dụng đủ đóng gói, kế thừa, đa hình, trừu tượng.
- Quản lý user, CRUD sản phẩm, tạo/tham gia/kết thúc phiên, kiểm tra bid, winner, vòng đời OPEN→RUNNING→FINISHED→PAID/CANCELED.
- Singleton AuctionManager, Factory Method tạo Item, Observer; xử lý đấu giá đồng thời, Socket realtime và server nhiều client.
- Custom exceptions InvalidBidException/AuctionClosedException/AuthenticationException; validation, lỗi dữ liệu/mạng/file.
- Serialization/File I/O, DAO; GUI tách logic khỏi Controller; JUnit, coverage≥60%, Maven/Checkstyle, GitHub Actions build/test.
- Commit thường xuyên, PR review, README, demo, slide, đóng góp thật; mỗi người hiểu toàn bộ mã nguồn.
- **Tùy chọn tuần13–14 nếu phần bắt buộc ổn định:** Auto-Bidding (maxBid, increment, PriorityQueue), Anti-sniping, biểu đồ giá realtime JavaFX LineChart. Không bắt buộc làm đủ cả ba. Payment gateway thật, email, cloud, database hay Spring không phải yêu cầu thêm của kế hoạch.

### Phân biệt đề bài và quyết định của nhóm

Số tuần, công nghệ, chức năng chính và tiêu chí trên lấy từ PDF. Cách chia bốn người, tên package/API, thứ tự công việc trong từng tuần, chiến lược khóa/snapshot và quy tắc quyền chi tiết bên dưới là hướng dẫn triển khai đề xuất. Nhóm phải duyệt và ghi lại trước khi code. PDF chưa quy định chính xác bước giá, cách thanh toán/hủy phiên, giới hạn dữ liệu hoặc quyền Admin chi tiết; không coi các quyết định đó là yêu cầu do giảng viên đã chốt.

PDF ghi tối đa +1.5 điểm ở trang1 nhưng tổng ở trang6 là 10+1; cần hỏi giảng viên nếu ảnh hưởng cách tính điểm. Không dùng chức năng cộng điểm để thay thế chức năng bắt buộc.

## 2. Phân công cố định và cân bằng cho 4 người

Không mặc định thành viên nào giỏi hơn. Tất cả đều có code backend, test module mình và một phần tích hợp/GUI. Không giao một người chỉ viết báo cáo hoặc chỉ chạy test của người khác.

| Người | Module chính tự code | File/package sở hữu | Vai trò chung | Hỗ trợ và reviewer |
|---|---|---|---|---|
| Thành viên 1 (TV1) | User/role, đăng ký/đăng nhập, CRUD Item, Factory Method | `model/user`, `model/item`, `factory`, `service/UserService`, `service/ItemService` và test tương ứng | Điều phối thiết kế OOP và quy tắc quyền; làm GUI quản lý Seller tối thiểu | TV4 hỗ trợ password/lưu trữ; TV2 review nghiệp vụ |
| Thành viên 2 (TV2) | Auction/Bid, AuctionManager Singleton, trạng thái, đặt giá, locking, tự đóng, Observer nghiệp vụ | `model/auction`, `service/AuctionService`, `service/AuctionManager`, `observer`, `scheduler` và test tương ứng | Chịu trách nhiệm kiến trúc nghiệp vụ; làm controller đấu giá tối thiểu | TV1 hỗ trợ kiểm tra owner/role; TV3 review concurrency/event |
| Thành viên 3 (TV3) | DTO/protocol, server Socket, session, dispatch command, client SDK, event qua mạng | `protocol`, `server`, `client/network`, test Socket | Tích hợp chính; làm danh sách/chi tiết qua client SDK và hỗ trợ GUI dùng API | TV2 hỗ trợ Observer; TV4 review lifecycle/network errors |
| Thành viên 4 (TV4) | DAO/Serialization, snapshot nhất quán, password utility, build/Checkstyle/Actions/JaCoCo | `persistence`, `security`, `pom.xml`, `checkstyle.xml`, workflow và test I/O | Điều phối kiểm thử và chất lượng; làm login/register FXML tối thiểu | TV1 review DAO/security; TV3 hỗ trợ E2E và CI |

TV2 chốt kiến trúc với cả nhóm, TV3 phụ trách ghép module, TV4 điều phối kiểm thử. Đây không có nghĩa họ phải tự làm thay ba người còn lại. Mọi PR phải có ít nhất một người review chéo.

**Nguyên tắc sở hữu file:** chủ module sửa file chính; người hỗ trợ tạo test hoặc đề xuất PR nhỏ. Các file dùng chung (enum, DTO, State, interface DAO, pom, FXML id) phải báo trước khi thay chữ ký. Không cùng sửa `AuctionManager` hoặc `pom.xml` trong hai nhánh dài ngày rồi chờ tuần 11 mới ghép.

### Cách chia thời gian mỗi tuần

Đầu tuần cả nhóm thống nhất số giờ phù hợp lịch học; PDF không đặt số giờ cố định mỗi người. Tuần6–8 ưu tiên model/logic song song học GUI; tuần9–10 ưu tiên Socket, lưu trữ và nối GUI; tuần11–12 ưu tiên tích hợp và sửa lỗi; tuần13–14 ưu tiên UX, regression và tài liệu; tuần15 dành demo/vấn đáp. Mỗi người luôn tự test phần code của mình. Nếu thiếu thời gian, bỏ chức năng tùy chọn trước, không bỏ GUI/Socket/Serialization/test bắt buộc.

## 3. Thiết kế và hợp đồng cần chốt ngay tuần 6

### 3.1. Cấu trúc khung — nhóm tự triển khai nội dung

```text
auction-system/
├── pom.xml
├── checkstyle.xml
├── README.md
├── .gitignore
├── .github/workflows/ci.yml.example  # TV4 tự viết YAML rồi đổi thành ci.yml ở tuần9
├── docs/                       # Ghi quyết định, API, test, đóng góp khi thực hiện
├── data/                       # Snapshot runtime, không commit tài khoản/mật khẩu
├── src/main/java/vn/uet/auction/
│   ├── model/user/             # User abstract, Bidder, Seller, Admin, Role — TV1
│   ├── model/item/             # Item abstract, 3 subclass, ItemType — TV1
│   ├── model/auction/          # Auction, Bid, AuctionStatus — TV2
│   ├── factory/                # ItemCreator và 3 concrete creator — TV1
│   ├── service/                # UserService, ItemService, AuctionService, AuctionManager
│   ├── exception/              # Custom exceptions — chủ nghiệp vụ tương ứng
│   ├── observer/               # AuctionListener, AuctionEvent — TV2
│   ├── scheduler/              # AuctionScheduler — TV2
│   ├── persistence/            # ApplicationState, StateStore, FileStateStore — TV4
│   ├── security/               # PasswordHasher — TV4
│   ├── protocol/               # Request, Response, EventMessage, DTO — TV3
│   ├── server/                 # ServerMain, AuctionServer, ClientHandler, Session — TV3
│   └── client/
│       ├── network/            # AuctionClient, receiver, request correlation — TV3
│       ├── controller/         # Login/Seller/Auction controllers — TV4/TV1/TV2
│       └── model/              # Client state/DTO mapping — TV3
├── src/main/resources/view/    # FXML, TV4 điều phối, mỗi người 1 màn hình
└── src/test/java/vn/uet/auction/
    ├── user/                   # TV1
    ├── item/                   # TV1
    ├── auction/                # TV2
    ├── network/                # TV3
    └── persistence/            # TV4
```

Khung tương ứng đã được tạo theo yêu cầu để nhóm tự code. File Java chỉ có ghi chú TODO, chưa có khai báo package/class/interface/enum hoặc method; FXML chưa có layout, các test chưa có assertion. pom.xml có metadata/JDK/UTF-8 ban đầu; dependencies/plugins và rules Checkstyle chờ TV4 hoàn thiện. CI hiện dùng đuôi .example để chưa kích hoạt workflow chưa viết. Danh sách file thực tế, chủ sở hữu và tuần thực hiện ở [docs/file-map.md](docs/file-map.md). Việc có file khung không có nghĩa một chức năng đã hoàn thành; trạng thái yêu cầu vẫn là Chưa làm cho đến khi nhóm tự triển khai và kiểm chứng.

### 3.2. Quyết định nghiệp vụ cần cả nhóm duyệt

Các điểm dưới đây là **đề xuất của kế hoạch vì PDF chưa mô tả chi tiết**, không phải quy định trích nguyên từ đề:

| Nội dung | Quy tắc tối thiểu đề xuất | Ai chốt / khi nào |
|---|---|---|
| Vai trò | Public register chỉ Bidder/Seller; Admin tạo bởi cấu hình server một lần | Cả nhóm, tuần 6 |
| Quản lý user | Đăng ký/login, xem thông tin an toàn; Admin xem danh sách, khóa/mở tài khoản; tài khoản khóa không được tiếp tục thao tác | TV1+TV3, tuần 6 |
| Tiền | BigDecimal, VND nguyên dương; mỗi bid lớn hơn giá hiện tại, kể cả bid đầu; giới hạn tối đa thống nhất | TV1+TV2, tuần 6 |
| Thời gian | Instant, giờ server là chuẩn; start < end, end ở tương lai; now==end thì không nhận bid | TV2+TV3, tuần 6 |
| Quyền Seller | Chỉ quản lý sản phẩm/phiên của mình; không tự bid | TV1+TV2, tuần 6 |
| Sửa/xóa Item | Cho phép khi chưa gắn phiên; từ chối sửa/xóa Item đã có phiên để giữ lịch sử | TV1+TV2, tuần 6 |
| Winner | Bid cao nhất đã chấp nhận khi đóng; không có bid thì không có winner | TV2, tuần 6 |
| PAID | Người thắng xác nhận thanh toán mô phỏng sau FINISHED, không chuyển tiền thật | TV2, tuần 6 |
| CANCELED | Chủ Seller hoặc Admin hủy OPEN/RUNNING/FINISHED chưa trả; không hủy PAID | TV1+TV2, tuần 6 |
| Dữ liệu | Một snapshot gồm users/items/auctions/bids; mã UUID; dữ liệu demo tách file thật | TV4+TV2, tuần 6 |
| Thông báo | Client nhận DTO không có password/hash, event chỉ báo dữ liệu đấu giá an toàn | TV3+TV1, tuần 6 |

Nếu giảng viên có quy định khác, cập nhật hợp đồng + test trước khi sửa code; không để mỗi người tự suy đoán một quy tắc.

### 3.3. Hợp đồng API tối thiểu

Tên bên dưới là đề xuất thống nhất cho nhóm tự triển khai. Chốt tên/kiểu thật vào cuối tuần 6 rồi giữ ổn định; các service có thể được gọi qua facade `AuctionManager`. Không biến controller JavaFX thành nơi chứa business rule.

| API dự kiến | Đầu vào | Đầu ra | Quyền / lỗi chính | Người sở hữu |
|---|---|---|---|---|
| `register` | username, password, Role | UserSummary hoặc userId | Trùng tên, input sai, cấm tự tạo Admin | TV1 |
| `login` | username, password | UserSummary | AuthenticationException; session do server gắn sau thành công | TV1; TV3 gắn session |
| `listUsers`, `setUserActive` | actor, username/active | Danh sách DTO hoặc void | Chỉ Admin; không khóa Admin cuối cùng | TV1 |
| `createItem` | actor, ItemType, name, description | itemId | Seller, validation | TV1 |
| `listItems`, `getItem` | actor, bộ lọc/id | List<ItemSummary>/ItemDetail | Quy tắc quyền và visibility chốt trước | TV1 |
| `updateItem`, `deleteItem` | actor, itemId, dữ liệu | void | Đúng chủ, Item chưa có phiên | TV1 |
| `createAuction` | actor, itemId, BigDecimal, start/end | auctionId | Đúng chủ, thời gian/giá đúng, không trùng phiên | TV2 |
| `listAuctions`, `getAuction` | bộ lọc/id | List<AuctionSummary>/AuctionDetail | DTO immutable; không trả State sống | TV2 |
| `placeBid` | actor, auctionId, BigDecimal | BidSummary/giá mới | Bidder, RUNNING, giá tăng; 2 custom exceptions | TV2 |
| `closeDueAuctions` | thời gian từ Clock | void hoặc list thay đổi | Tự chuyển trạng thái, không trả winner sai | TV2 |
| `confirmPayment`, `cancelAuction` | actor, auctionId | void | Winner/owner/Admin theo quy tắc đã duyệt | TV2 |
| `addListener`, `removeListener` | AuctionListener | void | Không để listener hỏng làm thất bại bid đã commit | TV2 |
| `StateStore.load`, `save` | rỗng / ApplicationState | ApplicationState / void | IOException; file hỏng không tự ghi đè rỗng | TV4 |
| `AuctionClient.sendRequest` | Command, payload DTO | Response/future | Mạng lỗi/timeout; không block FX thread | TV3 |

Mỗi hợp đồng phải bổ sung: null có được phép không, field bắt buộc, giới hạn độ dài, error code, kiểu tiền/thời gian, hành vi khi không tìm thấy. Ví dụ winner chưa có nên dùng Optional trong service hoặc field nullable được ghi rõ trong DTO; không dùng chuỗi rỗng để vừa chỉ “chưa có” vừa chỉ “lỗi”.

### 3.4. Giao thức tối thiểu để backend và GUI làm độc lập

- `Request`: requestId, command, payload; `Response`: requestId, success, errorCode, message, data; `EventMessage`: eventType, auctionId, dữ liệu an toàn hoặc tín hiệu yêu cầu refresh.
- Command tối thiểu: REGISTER, LOGIN, LIST_USERS, SET_USER_ACTIVE, CREATE_ITEM, LIST_ITEMS, GET_ITEM, UPDATE_ITEM, DELETE_ITEM, CREATE_AUCTION, LIST_AUCTIONS, GET_AUCTION, PLACE_BID, CONFIRM_PAYMENT, CANCEL_AUCTION.
- Event tối thiểu: BID_ACCEPTED, AUCTION_STARTED, AUCTION_FINISHED, AUCTION_CANCELED; có thể dùng event CHANGED và query lại nếu nhóm thống nhất. Event không chứa tài khoản/hash.
- Actor phải được lấy từ session server. Payload gửi `actor=admin` không được phép thay quyền người đang đăng nhập.
- TV3 quản lý schema/version. TV1/TV2 trả kết quả nghiệp vụ, TV3 map sang response; không đặt Socket vào User/Item/Auction.
- Tất cả thành viên có thể dùng test client console nhỏ của TV3 để nghiệm thu backend trước khi GUI đẹp; console chỉ là công cụ test, không thay GUI JavaFX bắt buộc.

## 4. TUẦN 6 — Thiết kế, OOP và nền dự án

**Theo PDF:** tuần 6, trang 2. Mốc kiểm tra nội bộ: **W6**.



### Việc cụ thể của từng người

| Người | Tự code và cách triển khai | File dự kiến | Test / đầu ra bàn giao |
|---|---|---|---|
| TV1 | Vẽ User/Item hierarchy; viết abstract class và 3 subclass cho mỗi nhóm. Field private, constructor kiểm tra tối thiểu, getter và override hành vi phù hợp. Viết `ItemCreator` có factory method được concrete creator override; chưa cần CRUD hoàn chỉnh. Chốt quyền, username và field Item với cả nhóm. | User/Bidder/Seller/Admin/Role; Item/Electronics/Art/Vehicle/ItemType; ItemCreator và 3 creator | Khởi tạo đủ 3 loại Item qua Factory, gọi qua kiểu cha; test tên thiếu/role; class diagram và mô tả public methods |
| TV2 | Viết Auction, Bid, AuctionStatus; xác định invariant giá/thời gian/winner. Thiết kế AuctionManager Singleton, quy định nơi giữ State/lock và cách service được test độc lập. Inject Clock vào logic thời gian thay vì rải `now()` khó test. Chốt bảng chuyển trạng thái, chưa viết hết bid. | model/auction; AuctionManager; bản thiết kế AuctionService | Test getter/invariant và instance Singleton; bảng trạng thái hợp lệ/sai; API tạo phiên/bid được cả nhóm duyệt |
| TV3 | Định nghĩa Request/Response/EventMessage và DTO an toàn theo các chữ ký đã thống nhất. Viết test dựng/đọc DTO; làm FakeAuctionGateway/test client in-memory để thử gọi hợp đồng, chưa làm server mạng hoàn chỉnh. Chuẩn bị danh sách command và error mapping. | protocol DTO, Command/ErrorCode; client test adapter | Ví dụ request/response cho login, createItem, createAuction, bid; DTO không lộ mật khẩu; tài liệu schema v1 |
| TV4 | Khởi tạo Maven/JDK thống nhất, folder main/test, JUnit cơ bản và .gitignore; khai báo StateStore và ApplicationState chứa aggregate maps. Viết MemoryStateStore cho test, chưa cần file thật. Chuẩn bị PasswordHasher API; cùng TV1 thống nhất dữ liệu user lưu hash. | pom.xml, .gitignore, persistence/ApplicationState, StateStore, MemoryStateStore trong test | `mvn test` chạy ở cả4 máy; test load/save memory độc lập; README bước setup ban đầu |

Maven được khởi tạo sớm để nhóm cùng build, nhưng phần tích hợp Checkstyle/Actions hoàn chỉnh vẫn đặt tuần 9 theo PDF. Không chờ đến tuần 9 mới có cách biên dịch chung.

### Thứ tự làm trong tuần

1. **Buổi đầu, cả nhóm:** đọc đủ PDF; duyệt scope, bảng quyền, tiêu chí nghiệm thu toàn hệ thống, danh sách câu hỏi chưa rõ. Tạo GitHub repo, main/dev, nhánh từng module và board công việc.
2. **Buổi 2:** TV1+TV2 chốt model, TV3 chốt DTO, TV4 chốt StateStore; cả nhóm review hợp đồng trước khi code sâu.
3. **Buổi 3–4:** bốn người code file sở hữu song song, dùng fake/memory ở biên phụ thuộc; không chờ một người xong cả module.
4. **Buổi 5:** ghép skeleton, sửa lỗi package/chữ ký, chạy `mvn test` trên cả4 máy; không merge code chưa compile.
5. **Cuối tuần:** review chéo; mỗi người giải thích một class không do mình viết.

### Hướng dẫn code trọng tâm

- Bắt đầu từ model thuần Java, không import JavaFX/Socket vào domain. Dùng BigDecimal và Instant, enum cho role/type/status, UUID cho mã định danh.
- Không lạm dụng kế thừa: chỉ tạo hierarchy đề yêu cầu và override có ý nghĩa; không thêm pattern chỉ để nhiều class.
- Singleton phải an toàn khi nhiều luồng gọi getInstance; chốt một cách đơn giản như synchronized. Logic nghiệp vụ nên nằm trong service có thể tạo riêng trong test, tránh test làm bẩn Singleton toàn JVM.
- Factory Method phải có creator cha và method được override; một `switch` trả Item đơn thuần chưa thể hiện đầy đủ Factory Method được đề nhắc tới.
- Chỉ sau khi chốt chữ ký mới chia nhánh song song. Những thay đổi enum/DTO được TV3 tổng hợp và báo cả nhóm.

### Nhánh JavaFX bắt buộc trong tuần

Cả bốn cài/chạy được JavaFX Hello World. TV4 làm FXML thử nghiệm và mở bằng SceneBuilder, TV3 giúp cấu hình Maven JavaFX; TV1/TV2 tự đổi một label/button và giải thích controller. Chỉ cần prototype, không làm giao diện đẹp trước nghiệp vụ.

### Checklist nghiệm thu W6

- [ ] Repo có main/dev và commit thật của cả4 người, không dồn một commit cuối kỳ.
- [ ] Model có đủ User/Item hierarchy, Auction/Bid, enums; Factory/Singleton được giải thích.
- [ ] Hợp đồng service/protocol/DAO được cả4 xác nhận; trách nhiệm file rõ ràng.
- [ ] Cả4 máy chạy được `mvn test` và JavaFX Hello World.
- [ ] Có test tối thiểu cho model/factory và fake/memory để tuần 7 không bị chờ DAO/Socket.

## 5. TUẦN 7 — Backend nghiệp vụ trong bộ nhớ, Observer và đa luồng

**Theo PDF:** tuần 7, trang 3. Mốc kiểm tra nội bộ: **W7**.



| Người | Tự code và cách triển khai | File dự kiến | Test / đầu ra bàn giao |
|---|---|---|---|
| TV1 | Hoàn thiện UserService register/login dựa PasswordHasher của TV4 hoặc fake đúng interface; quản lý role và khóa/mở tài khoản cho Admin. Viết ItemService create/list/get/update/delete, chặn sửa sản phẩm người khác và Item đã có phiên. Không tự cấp Admin qua register. | UserService, ItemService; user/item tests | Kịch bản đăng ký→login→Seller CRUD; sai password/trùng tên/quyền sai bị từ chối; dữ liệu đưa cho DTO không chứa hash |
| TV2 | Viết tạo phiên, bid, currentPrice, history, winner, pay/cancel; closeDueAuctions nhận Clock. Đặt ReentrantLock cho toàn thao tác đọc-kiểm tra-cập nhật. Viết Observer chỉ phát khi thao tác thành công; chuẩn bị scheduler interface, cho test chủ động tick trước. | AuctionService, AuctionManager, AuctionListener/AuctionEvent; auction tests | Tạo phiên→OPEN/RUNNING→bid→FINISHED→PAID; CANCELED đúng quyền; test nhiều thread giá tăng, bid muộn bị chặn |
| TV3 | Ghép test gateway với User/Item/Auction API, viết dispatcher in-memory và client console nhỏ gọi qua command. Mapping DTO/command đủ luồng login, CRUD, createAuction, bid, get/list; callback Observer được hiển thị ở test client. | protocol mapper; CommandDispatcher bản chưa Socket; console test client | Một kịch bản đi qua hợp đồng command thật, không gọi private methods; invalid command/payload có phản hồi rõ; chuẩn bị integration test |
| TV4 | Hoàn thiện PasswordHasher có salt; test đúng/sai password, cùng password tạo hash khác. Viết test helper Clock và memory store bản sao, fake save failure để chuẩn bị tuần 9. Hỗ trợ TV2 thiết kế test concurrency bằng executor/latch, không chỉ dùng sleep. | security/PasswordHasher; test fixtures; memory persistence tests | Utility hoạt động, không log password; fixtures dùng chung đã review; bộ test không dùng State sống chung giữa test |

### Thứ tự tích hợp

1. TV1 giao User/Item API ổn định đầu tuần; TV2 dùng DTO/model đã có từ W6, không chờ UI.
2. TV2 giao createAuction/bid bản một luồng giữa tuần để TV3 ghép dispatcher; sau đó thêm lock và Observer trong cùng PR nhỏ.
3. TV4 giao PasswordHasher sớm để TV1 thay fake bằng utility thật trước cuối tuần.
4. Cuối tuần, TV3 chạy kịch bản hợp đồng; TV2+TV4 chạy test đồng thời; TV1 review toàn bộ role check.

### Hướng dẫn code đặt giá — tự viết từ thuật toán

1. Kiểm tra amount có định dạng/giới hạn hợp lệ; xác định actor từ ngữ cảnh đã xác thực.
2. Vào cùng một lock bảo vệ auction/state. Đọc lại trạng thái và currentPrice **sau khi lấy lock**.
3. Kiểm tra role Bidder, phiên tồn tại, thời gian server và status RUNNING.
4. Kiểm tra amount > currentPrice; nếu sai ném lỗi nghiệp vụ, không thêm history.
5. Tạo Bid immutable, cập nhật giá/history nhất quán; không chỉ đổi mỗi currentPrice.
6. Nhả lock; phát event sau khi cập nhật thành công. tuần 9 sẽ nâng thành “save thành công rồi mới commit và event”.

Không viết `if (amount > currentPrice)` ở ngoài lock rồi chỉ khóa dòng gán. Không dùng double. Không để listener gọi Socket khi vẫn giữ lock nghiệp vụ.

### Bảng chuyển trạng thái phải test

| Tình huống | Trạng thái sau | Bid / payment |
|---|---|---|
| now < start | OPEN | Không nhận bid |
| start ≤ now < end | RUNNING | Nhận bid đúng role và giá |
| now ≥ end | FINISHED | Chặn bid, xác định winner nếu có |
| Người thắng xác nhận sau FINISHED | PAID | Không nhận bid/hủy/trả lần nữa |
| Chủ phiên/Admin hủy theo quy tắc đã chốt | CANCELED | Không nhận bid/payment |

Nếu server bỏ qua nhiều mốc thời gian, OPEN có thể được cập nhật qua RUNNING tới FINISHED trong cùng tick. Không làm trạng thái mắc ở RUNNING khi end đã qua.

### Nhánh JavaFX

TV4 tạo login/register prototype; TV3 dựng danh sách/chi tiết dùng DTO mẫu; TV1 dựng form Seller; TV2 dựng form nhập bid/status. Mục tiêu đọc dữ liệu mẫu và gọi fake gateway, chưa nhét business vào controller.

### Checklist W7

- [ ] CRUD và phân quyền backend trong memory đủ, không có user tự đăng ký Admin.
- [ ] Đặt giá và các chuyển trạng thái hoạt động; không có bid thì winner rỗng.
- [ ] Test cạnh tranh giá có assert giá cuối cao nhất và history tăng nghiêm ngặt; một số bid bị từ chối là bình thường.
- [ ] Observer không phát khi bid bị từ chối; một listener lỗi không làm đảo kết quả đã thành công.
- [ ] Kịch bản tổng thể chạy qua dispatcher/test client, các form JavaFX cơ bản có dữ liệu mẫu.

## 6. TUẦN 8 — Ngoại lệ, JUnit, refactor và chốt backend logic

**Theo PDF:** tuần 8, trang 3. Mốc kiểm tra nội bộ: **W8**.



| Người | Tự code và cách triển khai | File dự kiến | Test / đầu ra bàn giao |
|---|---|---|---|
| TV1 | Chuẩn hóa AuthenticationException, validation username/password/name/type; kiểm tra mọi đường CRUD/role. Tách logic hash khỏi UserService, loại code trùng. Hoàn thiện phản hồi khi không tìm thấy user/item, chủ khác, tài khoản khóa. | exception/AuthenticationException; UserService/ItemService tests | Test valid/invalid/rỗng/null/boundary; người gọi nhận code ổn định; không rò hash qua DTO |
| TV2 | Tạo InvalidBidException, AuctionClosedException; refactor vòng đời/tick/winner; bổ sung tests tại now==start/end, giá bằng hiện tại, quá giới hạn, trả trước đóng, hủy PAID, nhiều luồng. Kiểm tra một nguồn thời gian/lock nhất quán. | exception; AuctionService/AuctionManager; auction tests | Toàn bộ bảng trạng thái pass, regression concurrency; giải thích vì sao synchronized/lock đặt ở đó |
| TV3 | Hoàn thiện error mapping của dispatcher, requestId, payload validation; tách protocol DTO khỏi entity. Viết contract tests cho từng command và luồng session giả; chuẩn bị đọc/ghi ObjectStream ở sandbox học riêng, chưa nhận mạng là xong. | protocol; CommandDispatcher; contract tests | Cùng lỗi cho cùng request; thiếu field/command lạ không làm crash dispatcher; schema v1 chốt |
| TV4 | Thiết lập báo cáo JaCoCo sơ bộ, tổ chức test fixtures và test độc lập; viết persistence contract test dùng MemoryStateStore. Review tính Serializable của model và serialVersionUID cho tuần 9. Kiểm tra không lưu password thô. | pom JaCoCo; persistence tests; test fixtures | Danh sách coverage thiếu và tests cần thêm; `mvn test` pass sạch; chuẩn bị hợp đồng DAO cuối cùng |

### Hướng dẫn viết test có giá trị

- Mỗi test đi theo Arrange → Act → Assert; assert cả kết quả và dữ liệu không bị đổi khi thao tác lỗi.
- Không test private method bằng reflection để bám implementation; kiểm tra public business contract.
- Dùng Clock kiểm tra mốc giờ thay vì chờ vài phút. Dùng executor và latch/barrier cho concurrency; assert thứ tự giá tăng và không mất dữ liệu.
- Test user, item, auction ở module mình; TV4 không chịu trách nhiệm viết thay tất cả unit tests.
- Tách lỗi parse/protocol khỏi lỗi nghiệp vụ. Server tuần 9 sẽ map chúng thành response, không gửi stacktrace.
- Xác định convention null/Optional và error codes trước khi freeze API. Không `catch (Exception)` rồi trả success hoặc bỏ qua lỗi.

### Tích hợp và GUI theo PDF

Đầu tuần dùng test đỏ chỉ lỗi cần sửa; giữa tuần refactor giữ test xanh; cuối tuần TV3 chạy lại contract tests. Cả nhóm chuyển prototype sang **FXML + MVC**: controller chỉ lấy input/gọi gateway/render, model và DAO không import JavaFX. Form Seller/auction do chủ module hoàn thiện; TV4 ghép navigation/login, TV3 review client model. API sau W8 chỉ đổi bằng PR thông báo cả nhóm.

### Checklist W8

- [ ] Có đủ ba custom exception theo tên trong PDF và lỗi input rõ ràng.
- [ ] JUnit kiểm tra hợp lệ/sai/rỗng/biên, đóng phiên, role và concurrency.
- [ ] Không còn business rule chỉ tồn tại trong GUI; logic chạy không cần JavaFX.
- [ ] Mọi command có contract test; DTO không chứa entity mutable/hash.
- [ ] FXML/MVC các form chính đã có cấu trúc, gọi fake gateway được.
- [ ] Có báo cáo coverage hiện tại và danh sách phần còn thiếu; chưa tự nhận đạt ≥60% khi chưa đo.

## 7. TUẦN 9 — Socket, Serialization, Maven/Checkstyle và CI thật

**Theo PDF:** tuần 9, trang 4. Mốc kiểm tra nội bộ: **W9**.



| Người | Tự code và cách triển khai | File dự kiến | Test / đầu ra bàn giao |
|---|---|---|---|
| TV1 | Ghép đăng ký/login/CRUD với dispatcher và DAO chung. Giao cho TV3 dữ liệu session an toàn, không trả password/hash. Kiểm tra account lock được xét khi request tiếp theo, không chỉ lúc login. Hoàn thiện API list/get item để GUI không đọc file trực tiếp. | UserService/ItemService; user/item integration tests | Register/login và CRUD qua Socket lưu được; restart vẫn đăng nhập được; tài khoản khóa đang login bị chặn |
| TV2 | Ghép AuctionManager với snapshot transaction: copy state→apply→save→commit RAM→event. Thống nhất một lock bảo vệ cả thay đổi giá và snapshot. Viết scheduler có start/stop; mọi bid vẫn kiểm tra giờ ngay trong transaction. | AuctionManager transaction; AuctionScheduler; auction persistence tests | Bid đã báo success tồn tại sau restart; save lỗi không đổi RAM/không notify; scheduler tự đóng không cần client request |
| TV3 | Viết AuctionServer/ClientHandler/session, dispatcher nối service, client SDK Socket. Output stream tạo/flush trước input stream; đồng bộ write; reader phân biệt response/event theo requestId. Có thread pool, giới hạn kết nối hợp lý, close session khi client ngắt, timeout connect/request. | server/*.java; client/network/*.java; Socket integration tests | Hai client kết nối thật, register/login/list/create/bid được; sai input trả lỗi, disconnect không kéo sập server |
| TV4 | Viết FileStateStore load/save Serialization. Snapshot gồm đủ mọi entity/history; ghi file tạm cùng thư mục rồi atomic move, không ghi trực tiếp đè file chính. Cấu hình Checkstyle, JaCoCo và Actions checkout/setup-java/mvn verify/upload reports. | FileStateStore; pom/checkstyle/workflow; I/O tests | File round trip; thiếu file→state rỗng, hỏng file→báo lỗi không đè; lỗi ghi→transaction fail; Actions run đầu tiên trên GitHub |

### Thứ tự bàn giao để tránh nghẽn

1. Đầu tuần: TV4 giao FileStateStore bản round-trip; TV3 giao Socket echo/handshake độc lập.
2. TV2+TV4 thống nhất transaction trước khi cho bid ghi dữ liệu thật. Không để mỗi service tự save vào file riêng.
3. Giữa tuần: TV1+TV3 chạy register/login/CRUD qua Socket với file; TV2 ghép create/bid/scheduler.
4. TV3+TV4 chạy full path và restart; cả nhóm sửa lỗi trước PR về dev. Đưa Actions lên sớm trong tuần để còn thời gian sửa CI.

### Hướng dẫn code lưu trữ nhất quán

Luồng transaction cần nhóm tự hiện thực và review:

```text
lấy lock chung
    đọc State hiện tại, tạo bản sao độc lập
    kiểm tra và áp dụng thay đổi lên bản sao
    DAO ghi bản sao ra file tạm, đóng/flush, thay snapshot chính an toàn
    chỉ khi save thành công: công bố bản sao thành State hiện tại
nhả lock
phát Observer event cho giao dịch đã commit
```

- Không shallow-copy map rồi sửa cùng entity với State cũ; rollback sẽ không còn tác dụng. Dùng copy constructor có kiểm thử hoặc deep copy phù hợp quy mô bài tập.
- `load()` chỉ trả State rỗng khi file chưa tồn tại. File hỏng phải dừng/báo lỗi, không tự xóa và không tự ghi đè.
- Chốt đường dẫn và giới hạn snapshot; đường dẫn cấu hình được, không gắn đường dẫn máy cá nhân vào source.
- Đảm bảo mọi cập nhật user/item/auction đều qua cùng cơ chế transaction, tránh chỉ bid được bảo vệ còn xóa Item gây snapshot lệch.
- Hash mật khẩu có salt; không ghi password thô vào snapshot/log. ObjectInputFilter giới hạn kiểu/độ sâu/kích thước phù hợp khi đọc Serialization, đặc biệt từ Socket.

### Hướng dẫn Socket tối thiểu

- Một session giữ actor sau login; actor do client gửi không có quyền ghi đè session.
- Thread phục vụ client đọc request và trả response; không giữ lock business khi chờ mạng.
- Mọi write response/event trên một stream được đồng bộ; reset object stream nếu tái sử dụng để tránh object cache cũ.
- Response dùng requestId tương ứng; không giả định “gói thứ nhất là response” vì có event xen vào.
- Đóng stream/socket/executor có chủ đích khi shutdown. Một client mất mạng không làm server dừng.
- Có lỗi storage/network/validation rõ ràng; không biến lỗi lưu file thành response thành công.

### Nhánh GUI

TV4 thay login fake bằng client SDK; TV1 nối form Seller; TV3 nối list/detail; TV2 nối placeBid. Request chạy ngoài FX thread, cập nhật control qua Platform.runLater. Cuối tuần ít nhất login → list → bid chạy qua server thật; không gọi service server trực tiếp từ controller.

### Checklist W9

- [ ] Socket nhiều session thực sự, server xác thực/phân quyền.
- [ ] CRUD/create/bid ghi file thật và restart giữ dữ liệu.
- [ ] Save thất bại không commit RAM và không phát event thành công.
- [ ] File hỏng được giữ nguyên, thông báo rõ cho người chạy.
- [ ] `mvn verify` chạy Checkstyle và JUnit; có Actions run trên GitHub để theo dõi/sửa.
- [ ] GUI có một luồng chạy qua client SDK thật, không block FX thread.

## 8. TUẦN 10 — Nhiều client, realtime, hoàn tất API và coverage ≥60%

**Theo PDF:** tuần 10, trang 4. Mốc kiểm tra nội bộ: **W10**.



| Người | Tự code và cách triển khai | File dự kiến | Test / đầu ra bàn giao |
|---|---|---|---|
| TV1 | Rà mọi endpoint user/item: đúng chủ, input giới hạn, DTO an toàn, Admin quản lý tài khoản. Sửa các lỗ hổng quyền qua request mạng. Hoàn tất Seller GUI có create/read/update/delete và createAuction qua API. | User/Item service; handlers/mappers phối hợp TV3; Seller FXML/controller | Toàn bộ user/item commands có integration test; seller khác không sửa/xóa; Admin public register bị chặn |
| TV2 | Hoàn thiện close/start/pay/cancel và winner đúng thời gian; Observer đủ loại thay đổi. Kiểm thử concurrency khi đồng thời bid/tick/cancel; bảo đảm không có bid sau đóng. Nối UI trực tiếp vào cập nhật trạng thái/winner. | AuctionService/Manager/Scheduler; Observer; Auction controller | Không chỉ bid mới có event: client khác thấy start/finish/cancel; trả tiền đúng người/đúng trạng thái; lock regression pass |
| TV3 | Tích hợp Observer qua Socket: subscription/broadcast theo thiết kế, bảo vệ writer mỗi client; client receiver phân loại event và refresh. Xử lý timeout, failed re-login xóa identity cũ, client disconnected. Hoàn thiện SDK đủ tất cả commands, list/detail GUI. | Event broadcaster; ClientHandler/Session; AuctionClient; network tests | Server + ít nhất3 client nhận giá/trạng thái; một client ngắt không làm hỏng client khác; payload giả actor bị bỏ qua |
| TV4 | Bổ sung test persistence cả phiên/history/winner; data recovery sau shutdown/restart; kiểm tra atomic replace và cleanup temp. Đưa JaCoCo gate ≥60% vào verify, tổng hợp thiếu coverage để từng chủ module tự bổ sung. Hoàn thiện login/register FXML và Actions xanh. | FileStateStore/tests; pom gate; workflow; Login FXML/controller | Báo cáo coverage thực, không bỏ module khó để nâng tỷ lệ; GitHub build/test xanh; fixture cô lập và dữ liệu demo an toàn |

### Hướng dẫn code realtime

1. Manager chỉ phát event sau transaction đã commit.
2. Server chuyển event thành message an toàn, gửi bằng cùng cơ chế write synchronized với response.
3. Client reader nhận event trên luồng riêng; không đợi request tiếp theo mới cập nhật.
4. Model client refresh/sửa DTO, rồi cập nhật JavaFX bằng Platform.runLater.
5. Nếu dùng CHANGED + query lại: gộp các refresh đang chờ để tránh tạo vô hạn request; snapshot mới nhất là nguồn sự thật.
6. Không tin thứ tự event bằng thứ tự commit nếu event được gửi bất đồng bộ; query snapshot mới hoặc dùng version có quy tắc rõ.
7. Mất kết nối/timeout: báo trạng thái chưa chắc chắn; sau reconnect query trước khi retry bid/pay, vì server có thể đã commit nhưng response bị mất.

### Bài test nghiệm thu nhiều client

- Khởi động server trên port tạm, tạo Seller, Bidder A, Bidder B bằng giao thức thật.
- Seller mở phiên; A/B cùng nhận danh sách và chi tiết.
- A đặt giá 110, B nhận event và query thấy110; B đặt120, A nhận và thấy120.
- Cho nhiều request cạnh tranh; giá/history giữ invariant dù request đến lộn thứ tự.
- Không gửi thêm request khi chờ end; scheduler phải tự đóng, cả hai client nhận FINISHED và cùng một winner.
- Winner pay được; người khác không pay được; sau restart giá/history/status vẫn đúng.

### Nhánh GUI phải đạt theo PDF tuần10

Đủ các màn hình/chức năng tối thiểu: login/register (TV4), danh sách và chi tiết sản phẩm/phiên (TV3), đấu giá realtime (TV2), quản lý Seller (TV1). Có thể dùng tab/pane cho vừa phạm vi. MVC/FXML rõ ràng, báo lỗi đọc được. Cả bốn review luồng của người khác; TV3 điều phối tích hợp, không gánh toàn bộ giao diện.

### Checklist W10 — Kiểm tra theo yêu cầu tuần10

- [ ] Mọi command trong mục3.4 có handler thật và kiểm thử qua Socket; không còn handler placeholder.
- [ ] Nhiều client đồng thời, bid/state event tới client khác mà không bấm refresh.
- [ ] Scheduler tự đóng ngay cả khi không có request; winner/payment/cancel đúng.
- [ ] Serialization khôi phục đủ account/item/auction/bid/winner/status.
- [ ] Coverage toàn mã chính ≥60% theo phạm vi đã công bố; ghi rõ line/branch, không lấy tổng số test thay coverage. 
- [ ] Actions build/test thực sự xanh; `mvn verify` không chỉ chạy được trên máy người viết.
- [ ] Các màn hình chính chạy qua server thật. Không thêm chức năng cộng điểm trong tuần 11.

## 9. TUẦN 11 — Tích hợp GUI, logic, mạng và dữ liệu

**Theo PDF:** trang4–5, nửa đầu giai đoạn tuần11–12; không còn bài giảng. **Mốc W11:** luồng toàn hệ thống chạy bằng thành phần thật và có danh sách lỗi tích hợp tái hiện được. Không đặt hạn riêng kết thúc backend trong tuần này.

| Người | Việc tự code / hướng dẫn triển khai | File/package chính | Test và đầu ra bàn giao |
|---|---|---|---|
| TV1 | Nối đầy đủ đăng ký/login/quản lý user và Seller CRUD với session/DAO thật. Sửa lỗi mapping/validation phát hiện khi dùng GUI; không chuyển role check sang controller. Bổ sung regression cho sản phẩm đã có phiên, tài khoản khóa và chủ khác. | UserService, ItemService, Seller controller/FXML, user/item integration tests | GUI tạo/sửa/xóa đúng quyền; response không có hash; tài khoản đang login bị khóa không tiếp tục thao tác |
| TV2 | Tích hợp scheduler, bid, winner, pay/cancel với transaction và Observer. Kiểm tra mọi mutation dùng cùng cơ chế đồng bộ/snapshot. Sửa khi event/state GUI lệch kết quả server; giữ server là nguồn thời gian. | AuctionManager/AuctionService, scheduler, observer, Auction controller | Phiên tự chạy/đóng không cần người dùng bấm; hai Bidder thấy cùng giá/winner; lỗi bid không thêm history |
| TV3 | Ghép server, client SDK và mọi màn hình qua Socket; loại đường gọi service trực tiếp từ GUI. Xử lý requestId/event xen kẽ, lifecycle session, disconnect. Điều phối lần E2E đầu bằng server+ít nhất3 client. | server, protocol, client/network, list/detail controller | Biên bản E2E lần1, log không có secret, mỗi lỗi có bước tái hiện/owner; một client ngắt không làm sập server |
| TV4 | Hoàn thiện schema snapshot khi ghép toàn hệ thống; viết test lưu rồi tải đủ User/Item/Auction/Bid/winner/status. Kiểm tra rollback khi save lỗi và giữ file hỏng để xử lý, bổ sung CI chạy integration tests ổn định. | persistence, test fixtures, I/O/integration tests, workflow | Restart dữ liệu thật được; lỗi file không gây success giả; báo cáo test/coverage hiện tại và lỗi CI cụ thể |

### Trình tự trong tuần

1. Đầu tuần, TV3 cùng nhóm lập ma trận GUI action→command→service→DAO→response/event, tìm chỗ còn dùng fake hoặc chưa có handler thật.
2. Ghép theo lát chức năng: register/login→Seller CRUD→tạo phiên→bid→tự đóng→payment/cancel→restart. Chủ module sửa phần mình, TV3 điều phối giao diện giữa các phần.
3. Chạy E2E với server thật, file tạm riêng và nhiều client. Console client có thể hỗ trợ debug nhưng không thay kiểm thử GUI.
4. Cuối tuần review lỗi P0/P1: mất dữ liệu, sai quyền, giá lùi, bid sau đóng, crash/treo. Không mở chức năng tùy chọn khi các lỗi này còn tồn tại.

### Hướng dẫn code tích hợp

- Giữ cùng kiểu tiền/thời gian và cách xử lý null giữa DTO/service/DAO. Bất kỳ thay đổi schema phải được TV3+TV4 review và cập nhật contract test.
- Với bid: lấy lock→đọc state mới nhất→check role/time/price→áp dụng lên bản sao→save→commit RAM→nhả lock→notify. Không gửi event thành công trước khi lưu xong.
- Với GUI: capture input trên FX thread, gửi request trên executor, đưa cập nhật control về Platform.runLater. Không gọi request đồng bộ từ callback reader đang nhận response.
- Khi timeout, không coi là giao dịch chắc chắn chưa xảy ra; reconnect rồi query kết quả trước retry. Ghi rõ giới hạn này trong hướng dẫn sử dụng.

### Checklist W11

- [ ] GUI↔Logic↔Network↔Data dùng thành phần thật, không còn fake ở luồng demo.
- [ ] Có biên bản E2E lần1 và lỗi được giao đúng owner.
- [ ] Tự đóng/winner và realtime cùng chạy; restart khôi phục đủ dữ liệu.
- [ ] CI được theo dõi và lỗi được xử lý; không chỉ có YAML mà không có run.

## 10. TUẦN 12 — Hoàn thiện tích hợp, edge cases, Serialization và CI xanh

**Theo PDF:** trang5, nửa sau giai đoạn tuần11–12. **Mốc W12:** bản tích hợp bắt buộc ổn định, đã test E2E nhiều client, lưu trữ hoàn thiện và Actions xanh. Đây là mốc của giai đoạn tích hợp trong PDF, không phải hạn backend rút gọn do kế hoạch thêm vào.

| Người | Việc tự code / hướng dẫn triển khai | File/package chính | Test và đầu ra bàn giao |
|---|---|---|---|
| TV1 | Sửa triệt để lỗi user/item từ W11, thêm test trước khi fix. Rà mọi API đúng chủ/role, input rỗng/dài/sai; kiểm tra CRUD không phá phiên đã tồn tại. Hoàn thiện thao tác Admin đã chốt và hướng dẫn Seller. | User/Item services, controllers, tests | Toàn bộ test quyền/CRUD đạt, không có bypass qua payload tự khai actor; hướng dẫn chức năng khớp code |
| TV2 | Kiểm tra race bid với tick/cancel/payment, mốc giờ start/end, phiên không có bid, server nghỉ qua thời điểm kết thúc. Sửa lỗi transaction và trạng thái, test lại scheduler start/stop. | Auction core, scheduler, observer, concurrency/lifecycle tests | History/giá/winner đúng; không bid muộn; payment đúng người và không lặp; shutdown không rò thread |
| TV3 | Sửa lỗi mạng/reconnect/request/event từ E2E; test server nhiều client và client rời giữa chừng. Thử kết nối trên máy khác nếu có điều kiện; ghi rõ chưa test LAN nếu chưa làm. Đóng băng contract sau khi sửa lỗi để tuần13 không thay API tùy tiện. | server/client SDK/protocol, integration tests | E2E lần2 pass bằng GUI thật; reconnect có hướng dẫn; handler command và DTO đồng nhất |
| TV4 | Kiểm tra atomic replace, file thiếu/hỏng/không ghi được, snapshot qua restart và dữ liệu mẫu. Chạy verify từ clone sạch, ổn định tests/Actions; đối chiếu coverage≥60% và phân công chủ module bù phần thiếu. | FileStateStore, I/O tests, pom, Checkstyle/JaCoCo, workflow | Report mới và URL Actions xanh; restart giữ dữ liệu; file hỏng không bị ghi đè rỗng; không secret/data thật trong Git |

### Kịch bản E2E cuối giai đoạn tích hợp

1. Clone/build từ máy khác hoặc thư mục sạch, khởi động server với file demo riêng chưa tồn tại.
2. Register/login Seller và hai Bidder; kiểm tra trùng tên, sai mật khẩu, tự đăng ký Admin và account khóa.
3. Seller CRUD đủ3 loại Item; người khác không sửa/xóa; Item gắn phiên được bảo vệ theo quy tắc nhóm.
4. Tạo phiên tương lai, kiểm tra chuyển OPEN→RUNNING; gửi bid cạnh tranh, xác nhận realtime tới client khác và history tăng.
5. Không gửi thêm request khi chờ hết giờ: scheduler tự đóng; hai Bidder thấy cùng winner; bid muộn bị từ chối.
6. Người thua không pay, winner pay thành PAID; kiểm tra CANCELED trên phiên khác, từ chối hủy/đặt giá ở trạng thái không hợp lệ.
7. Tắt/restart server cùng file: tài khoản, sản phẩm, phiên, bid, winner/status được khôi phục.
8. Ngắt một client, reconnect/login/query lại; client khác và server vẫn hoạt động; GUI không treo.
9. Mô phỏng lỗi file trên dữ liệu thử: giữ snapshot cũ, RAM không commit khi save lỗi, không thông báo thành công giả.
10. Chạy verify, đọc test report/coverage/Checkstyle và mở Actions run thật. Ghi PASS/FAIL/chưa test cho từng bước.

### Hướng dẫn chốt tích hợp

Sửa lỗi nào phải bổ sung test tái hiện lỗi đó; không refactor kiến trúc rộng chỉ vì muốn đổi tên. Ưu tiên lỗi quyền, dữ liệu, concurrency và crash trước lỗi trình bày. Reviewer chéo theo bảng phân công. Chỉ xác nhận ổn định khi có kết quả chạy thực tế; nếu còn lỗi chặn, ghi đúng trạng thái và dành tuần13 sửa phần bắt buộc thay vì triển khai tùy chọn.

### Checklist W12

- [ ] E2E nhiều GUI client qua Socket/file thật đạt; không chỉ test mock.
- [ ] Edge cases và lỗi I/O/network được kiểm tra có bằng chứng.
- [ ] Serialization hoàn thiện và restart không mất giao dịch đã báo thành công.
- [ ] Maven/Checkstyle/JUnit/coverage≥60% đạt, Actions thực sự xanh trên GitHub.
- [ ] Không còn lỗi chặn trước khi nhóm quyết định làm chức năng nâng cao.

## 11. TUẦN 13 — Polish GUI/UX, kiểm thử sâu và chọn chức năng nâng cao nếu kịp

**Theo PDF:** trang5, nửa đầu giai đoạn tuần13–14. **Mốc W13:** UX tốt hơn, regression bắt buộc giữ xanh; chỉ thêm tùy chọn nếu W12 đã ổn định. Không yêu cầu nhóm phải làm đủ cả ba tùy chọn.

| Người | Việc tự code / hướng dẫn triển khai | File/package chính | Test và đầu ra bàn giao |
|---|---|---|---|
| TV1 | Cải thiện Seller/User UX: input rõ, lỗi theo field, trạng thái loading/empty, chống thao tác nhầm. Nếu nhóm chọn auto-bidding, làm service/config maxBid/increment, phối hợp TV2 chốt tie-break trước; không tự tạo thuật toán ngoài lock bid. | Seller/User controllers/FXML/tests; auto-bid service nếu được chọn | CRUD dễ dùng hơn; test input/quyền không giảm. Tùy chọn chỉ được nhận khi có rule và test cụ thể |
| TV2 | Cải thiện trạng thái/countdown/winner trong màn đấu giá theo giờ server. Nếu chọn anti-sniping, thêm gia hạn end trong cùng transaction đặt giá, không đổi lịch ở client; review thuật toán auto-bid của TV1. | AuctionService/Manager/Scheduler/controller; tests thời gian | GUI phản ánh giờ đúng; nếu gia hạn có test trước/trong/sau ngưỡng, event end mới và giới hạn gia hạn |
| TV3 | Cải thiện list/detail/realtime, thông báo reconnect, lựa chọn phiên sau refresh. Nếu chọn LineChart, lấy BidHistory DTO, cập nhật series trên FX thread, tránh nhân đôi điểm khi refresh. | client model/network/controllers/FXML, LineChart nếu chọn | Giao diện không nhảy mất lựa chọn; chart ban đầu và realtime khớp lịch sử; không block FX thread |
| TV4 | Viết thêm tests cho UX/controller và tính năng tùy chọn đã chọn; kiểm tra persistence khi thêm auto-bid config hoặc end thay đổi. Cập nhật coverage/CI, review schema và chuẩn bị README cài/chạy trên máy sạch. | persistence/tests, CI, docs/README | Core regression và quality gate vẫn xanh; restart giữ dữ liệu mới nếu cần; không chỉ viết tài liệu thay code/test |

### Quyết định phạm vi đầu tuần

Nếu còn lỗi bắt buộc, cả4 dành tuần này sửa lỗi và UX, không bắt đầu nâng cao. Nếu ổn định, chọn tối đa số tính năng vừa sức, giao owner/reviewer/test rõ; PDF coi chúng là tùy chọn. Tạo nhánh riêng, chỉ merge khi integration tests và snapshot compatibility đạt.

### Hướng dẫn code nâng cao khi được chọn

- **Auto-bidding:** chốt maxBid, increment, thứ tự ưu tiên khi cùng mức tối đa; PriorityQueue phục vụ chiến lược nhưng mọi bid tự động vẫn phải đi qua kiểm tra/lock/transaction chung. Chặn vòng lặp bot tự đẩy vô hạn, không vượt maxBid. Test hai bot, tie, user thường và phiên đã đóng.
- **Anti-sniping:** chốt cửa sổ cuối và độ dài gia hạn; kiểm tra bằng Clock. Cập nhật end và scheduler theo nguồn dữ liệu server; GUI nhận thời gian mới qua event. Không gia hạn sau khi phiên đã FINISHED hoặc cho lần bid bị từ chối.
- **LineChart:** nạp lịch sử trước, hợp nhất event mới theo định danh/time đã chốt; không duplicate sau reconnect. Chart chỉ trình bày, không tự tính winner hoặc currentPrice thay server.
- Thay schema phải có kế hoạch dữ liệu demo/backup rõ; không xóa file thật để che lỗi deserialize.

### Checklist W13

- [ ] Login/list/detail/Seller/bidding có thông báo lỗi/loading/empty rõ, thao tác GUI không treo.
- [ ] Các lỗi bắt buộc từ W12 đã xử lý trước khi merge tính năng cộng điểm.
- [ ] Có quyết định chọn/không chọn từng tùy chọn, không nhận “đã làm” chỉ vì có nút trên GUI.
- [ ] Mọi tùy chọn được code đều có test nghiệp vụ/mạng/lưu trữ phù hợp và không phá regression.

## 12. TUẦN 14 — Hoàn thiện, regression, README và chuẩn bị bản demo

**Theo PDF:** trang5–6, nửa sau giai đoạn tuần13–14. **Mốc W14:** phiên bản bàn giao ổn định, README đầy đủ trên GitHub, minh chứng kiểm thử và demo sẵn sàng trước tuần15.

| Người | Việc tự code / hướng dẫn triển khai | File/package chính | Test và đầu ra bàn giao |
|---|---|---|---|
| TV1 | Sửa lỗi user/item/auto-bid nếu đã chọn, thêm regression. Rà validation/role và form Seller trên độ phân giải máy demo. Viết phần cài tài khoản, CRUD, quy tắc quyền và đóng góp thật của mình; chuẩn bị sơ đồ User/Item/Factory cho slide. | User/Item code/tests/controllers; README phần user/Seller; class diagram | Không lỗi chặn, hướng dẫn khớp hành vi thật; các PR/commit/test của TV1 được liệt kê đúng |
| TV2 | Chạy lại concurrency/lifecycle/payment/cancel và anti-sniping nếu có; sửa lỗi phát hiện. Kiểm tra timer shutdown và thời gian demo. Chuẩn bị minh họa lock/transaction/Observer và giải thích các quyết định thiết kế. | Auction core/tests/scheduler; sơ đồ trạng thái/transaction | Giá/winner/history đúng khi nhiều client; không giữ chức năng tùy chọn đang lỗi để lấy điểm |
| TV3 | Chốt server/client launch/config/reconnect, kiểm tra demo Server+nhiều Client. Đóng gói cách chạy bằng Maven/JAR phù hợp, cập nhật protocol và chỉ dẫn khi mạng lỗi. Chuẩn bị sơ đồ kiến trúc và kịch bản demo. | server/client/network/tests; README run/config/API; demo checklist | Thành viên khác làm theo hướng dẫn chạy được; client reconnect rõ; không phụ thuộc đường dẫn máy TV3 |
| TV4 | Chạy full verify từ clone sạch, lưu reports/URL Actions, soát secret và file runtime bị commit. Kiểm tra backup/restart/restore trên dữ liệu demo, phối hợp hoàn tất README, kiểm tra bản bàn giao/tag nội bộ. | persistence/security/tests/CI; reports/README; checklist bàn giao | CI xanh, coverage≥60%, test report mới; snapshot thật không vào Git; bản demo có backup và dữ liệu riêng |

### Thứ tự cuối giai đoạn

Đầu tuần chốt tính năng; giữa tuần chạy toàn bộ test và demo có người khác làm theo README; cuối tuần sửa lỗi còn lại, chạy lại test bị ảnh hưởng và kiểm tra bản cuối. Không thêm tính năng lớn sát buổi trình bày. Nhóm có thể bỏ một tùy chọn chưa ổn định qua PR có review, giữ bản bắt buộc hoạt động tốt.

### Hướng dẫn chuẩn bị bàn giao

README phải có JDK/Maven/JavaFX setup, clone/build/test, chạy server và nhiều client, host/port/path cấu hình, tạo tài khoản demo/Admin theo cách đã chốt, mô tả sử dụng, dữ liệu lưu ở đâu, backup/restore và giới hạn chưa làm. Kiểm tra từ thư mục sạch, không lấy cache/IDE config trên máy một người làm điều kiện ngầm. Không khẳng định đã test LAN/OS khác nếu chưa thực hiện.

Slide nháp gồm mục tiêu/phạm vi, kiến trúc Client–Server+MVC, OOP/patterns, concurrency/transaction, lưu trữ, GUI/demo, test/CI, đóng góp thật, giới hạn. Điền số test/coverage từ report hiện tại, không từ phiên bản cũ. Tất cả cùng review để mỗi người hiểu các slide ngoài phần mình.

### Checklist W14

- [ ] Phần bắt buộc đạt; tùy chọn giữ lại đều đã kiểm thử.
- [ ] Full verify/Actions xanh, reports và yêu cầu còn thiếu được ghi trung thực.
- [ ] README hoàn chỉnh trên GitHub, người khác chạy theo được.
- [ ] Server+nhiều GUI client, backup/demo data và phương án lỗi mạng đã diễn tập.
- [ ] Có slide nháp và bằng chứng đóng góp thật, sẵn sàng vấn đáp chéo.

## 13. TUẦN 15 — Trình bày, demo và vấn đáp

**Theo PDF:** trang5. **Mốc W15:** trình bày và chấm điểm; mỗi thành viên giải thích được mọi phần mã nguồn, demo Server+nhiều Client và phân chia điểm theo đóng góp thực tế.

| Người | Việc cụ thể trong tuần | Tài liệu / phần demo chuẩn bị | Điều kiện hoàn thành |
|---|---|---|---|
| TV1 | Chốt phần User/Item/Factory và quyền; chuẩn bị dữ liệu Seller, diễn tập CRUD và lỗi quyền. Review lại network/persistence để trả lời ngoài module mình; sửa lỗi chặn ở module mình nếu phát hiện, không đổi kiến trúc lớn. | Slide OOP/class diagram/quyền; kịch bản Seller; minh chứng PR/commit | Giải thích được vì sao abstract/override/Factory được dùng; tự demo được phần mình và hiểu tổng thể |
| TV2 | Trình bày Auction/Bid, Singleton/Observer, lock/vòng đời/winner; phối hợp demo hai Bidder và tự đóng. Luyện giải thích/rewrite một rule bid theo yêu cầu khác. | Slide concurrency/state/transaction; tests biên/race; demo winner | Giải thích được race condition, rollback, bid muộn, và đọc được module của3 người còn lại |
| TV3 | Vận hành server+nhiều client, thuyết trình kiến trúc/MVC/Socket/session/realtime. Chuẩn bị port/config/cách khởi động lại và phương án lỗi mạng; không chỉ TV3 biết chạy demo. | Slide kiến trúc/protocol; lệnh chạy; demo checklist và log an toàn | Bất kỳ thành viên nào cũng khởi động được bản demo; request/response/event được giải thích đúng |
| TV4 | Trình bày Serialization/DAO, exceptions/tests/coverage/CI; mở report và Actions thật. Chuẩn bị bản backup/demo file và đối chiếu yêu cầu; kiểm tra lại toàn bộ nguồn để vấn đáp chéo. | Slide lưu trữ/chất lượng; report/run URL; bảng đóng góp/kiểm thử | Chứng minh kết quả bằng lần chạy thật, giải thích được file hỏng/save lỗi và giới hạn kiểm thử |

### Trình tự diễn tập và trình bày

1. Trước buổi chấm, chạy đúng revision/tag dự kiến demo; kiểm tra server, JavaFX, port và dữ liệu mẫu. Chuẩn bị dependencies trước nếu nơi trình bày không có mạng; không cần deploy Internet nếu đề không yêu cầu.
2. Demo Seller tạo sản phẩm/phiên; hai Bidder đặt giá và nhận cập nhật, thử một lỗi giá/quyền, chờ tự đóng và kiểm tra winner/payment mô phỏng.
3. Khi cần chứng minh lưu trữ, restart cùng file demo và query lại history/status; mở report JUnit/coverage và Actions run tương ứng revision.
4. Mỗi người trình bày phần được giao nhưng phải trả lời được mọi module; đổi vai diễn tập để tránh chỉ thuộc lời thoại riêng.
5. Bảng đóng góp ghi PR/commit/test/review thật; nhóm phân chia điểm theo đóng góp thực tế như PDF, không dùng lịch phân công dự kiến làm bằng chứng đã làm.

### Câu hỏi vấn đáp chéo

- Actor đến từ session hay payload? Tài khoản khóa giữa phiên xử lý thế nào?
- Vì sao currentPrice/history phải được cập nhật trong cùng lock? Nếu hai giá đến lộn thứ tự thì sao?
- Clock/scheduler xử lý now==end và restart sau end như thế nào?
- Save lỗi thì RAM/response/event ra sao? File hỏng có bị ghi đè rỗng không?
- Event xen response được nhận diện thế nào? Vì sao GUI dùng Platform.runLater?
- Factory Method/Singleton/Observer ở đâu trong code? MVC tách những phần nào?
- Test nào dùng fake, test nào dùng Socket/file/GUI thật? Coverage≥60% có chứng minh hết lỗi không?

### Checklist W15

- [ ] Slide kiến trúc/demo/đóng góp khớp revision đang chạy.
- [ ] Demo Server+nhiều Client đã diễn tập và chạy được theo hướng dẫn.
- [ ] Cả4 giải thích được mọi module, không chỉ phần được phân công.
- [ ] Minh chứng test/CI và đóng góp thật sẵn sàng; không tuyên bố điều chưa xác minh.

## 14. Quy trình làm việc áp dụng từ tuần 6 đến tuần 15

### Git và phối hợp hằng tuần

- `main`: bản ổn định đã nghiệm thu; `dev`: tích hợp; mỗi người có nhánh tính năng ngắn từ dev, ví dụ `feature/user-item`, `feature/auction-core`, `feature/socket-protocol`, `feature/persistence-ci`.
- Đầu buổi: kiểm tra `git status`, đồng bộ dev; không ghi đè file người khác, không reset hard/force push nhánh chung.
- Commit mỗi phần việc có ý nghĩa, dùng Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `ci:`). Không chia commit giả để tạo đóng góp.
- PR ghi điều thay đổi, API bị ảnh hưởng, test/lệnh/kết quả. Reviewer theo bảng phân công. Chỉ merge khi compile/test liên quan pass; từ tuần 9 phải qua CI.
- Mỗi tuần tối thiểu hai lần ghép vào dev: giữa tuần ghép API/chức năng nhỏ, cuối tuần nghiệm thu. Không chỉ merge lần đầu ở tuần 11.
- Nếu đổi file chung: báo nhóm, duyệt chữ ký, một người sửa, cập nhật caller/test trong cùng PR. Không để một nhánh dùng `double` còn nhánh khác dùng BigDecimal.
- Conflict được chủ file và người tích hợp đọc từng phần; không tự chọn toàn bộ ours/theirs. Sau giải quyết phải build lại.

### Nhịp họp ngắn

Đầu tuần chốt đầu ra và dependency; giữa tuần demo tiến độ qua test; cuối tuần demo mốc W6–W15 và vấn đáp. Mỗi ngày làm việc cập nhật ba dòng: đã làm gì, tiếp theo làm gì, đang vướng hợp đồng/file nào. Một vướng mắc kéo dài quá một buổi phải trao đổi, không im lặng chờ người khác xong.

### Quy tắc chất lượng chung

- Java/package/method naming thống nhất; UTF-8, indent4 spaces, một public class/file; comment giải thích lock/I/O/quyết định nghiệp vụ.
- Tiền/time/config có kiểu và giới hạn rõ; path/port không hard-code theo máy cá nhân.
- UI validate để hỗ trợ người dùng, backend vẫn validate tất cả vì client có thể bị sửa.
- Test theo module do chủ module viết; TV4 tổng hợp quality gate. Test dùng fixture cô lập, temp directory và Clock, không dùng data thật.
- Không công khai password/hash/token trong response/log/Git. Dữ liệu demo và snapshot thật tách biệt.
- Mỗi tuần một người review một module khác; cuối tuần giải thích 5–10 phút về phần mình và một phần người khác. PDF yêu cầu mỗi người hiểu toàn bộ mã nguồn, không chỉ module phụ trách.

### Rủi ro và cách xử lý trong toàn bộ lộ trình

| Rủi ro | Dấu hiệu | Cách xử lý | Người điều phối |
|---|---|---|---|
| API lệch nhau | Không compile khi ghép hoặc map DTO tùy tiện | Freeze contract W6/W8, contract tests; PR đổi API sửa caller cùng lúc | TV3 |
| TV2 quá tải nghiệp vụ/lock | Chưa có bid một luồng giữa tuần 7 | TV1 hỗ trợ quyền/validation, TV4 viết test race, TV3 tách transport; TV2 tập trung transaction | TV2 |
| TV4 quá tải DAO/CI | Cuối tuần 9 chưa có round trip | TV1 hỗ trợ copy/state schema, TV3 hỗ trợ workflow/test runner; chủ module tự bổ sung tests thay vì đẩy TV4 | TV4 |
| Học GUI quá muộn | tuần 8 chưa có FXML/controller chạy | Chia4 màn hình theo domain, mỗi tuần có đầu ra UI; không giao toàn bộ cho TV3 | TV4 điều phối UI |
| Mất dữ liệu khi tích hợp | RAM báo thành công nhưng restart mất bid | Một transaction/lock chung, save-before-commit, tests lỗi ghi; ưu tiên hơn polish | TV2+TV4 |
| Realtime làm treo app | GUI request chạy FX thread, write stream xen kẽ | Network executor + Platform.runLater, writer đồng bộ, client reader riêng | TV3 |
| CI chỉ tồn tại trên giấy | Có YAML nhưng không có run hoặc luôn đỏ | Push/check Actions ngay tuần 9, JDK/dependency thống nhất, lưu run URL | TV4 |
| Backend chưa xong mà làm tùy chọn | tuần 10 còn handler trống nhưng bắt đầu auto-bid | Ưu tiên phần bắt buộc, chỉ cân nhắc tùy chọn từ tuần13 khi W12 ổn định, giao lỗi rõ owner | Cả nhóm |

## 15. Bảng đối chiếu yêu cầu và nghiệm thu toàn dự án

Các tuần dưới đây là mốc triển khai/kiểm tra theo lộ trình PDF. Phần phát triển chính ở tuần6–10 được tiếp tục tích hợp/sửa lỗi tại11–12, kiểm thử hoàn thiện tại13–14, trình bày ở15. Không đặt một hạn riêng cho backend. Tất cả trạng thái hiện là **Chưa làm** vì nhóm sẽ tự code; kết quả của bản dự án trước reset không được dùng làm bằng chứng cho bản mới.

| Mã | Hạng mục / mức độ | Tuần triển khai và rà soát | Chủ chính | Bằng chứng hoàn thành | Trạng thái |
|---|---|---|---|---|---|
| B01 | User/Item hierarchy, Auction/Bid, đủ4 tính chất OOP — bắt buộc | 6; rà soát8/14 | TV1+TV2 | Code, class diagram, giải thích override/encapsulation | Chưa làm |
| B02 | Singleton AuctionManager, Factory Method — bắt buộc | 6; rà soát8 | TV2/TV1 | Pattern đúng ý nghĩa, test phù hợp | Chưa làm |
| B03 | User Bidder/Seller/Admin, register/login/phân quyền — bắt buộc | 7–10; E2E11–12 | TV1+TV3 | Tests valid/invalid/role/session, DTO an toàn | Chưa làm |
| B04 | CRUD Item đủ3 loại, đúng chủ — bắt buộc | 7–10; E2E11–12 | TV1 | CRUD tests và GUI, người khác bị chặn | Chưa làm |
| B05 | Tạo phiên, bid hợp lệ, currentPrice/history — bắt buộc | 7–8; nối mạng9–10 | TV2 | Tests giá đúng/sai/rỗng/biên, Socket và GUI | Chưa làm |
| B06 | Vòng đời, tự đóng/winner, pay/cancel theo rule nhóm — bắt buộc | 7–10; edge cases11–12 | TV2 | Clock/scheduler tests, winner thống nhất, lỗi trạng thái | Chưa làm |
| B07 | Concurrent bidding, không lost update/race — bắt buộc | 7; regression8/10–12/14 | TV2, TV4 hỗ trợ test | Giá/history nhất quán dưới nhiều thread/client | Chưa làm |
| B08 | Observer trong logic, realtime qua Socket — bắt buộc | 7 Observer; 10 qua mạng; E2E11–12 | TV2+TV3 | Client khác nhận bid/state không refresh thủ công | Chưa làm |
| B09 | InvalidBidException/AuctionClosedException/AuthenticationException — bắt buộc | 8; mapping9–10 | TV1+TV2+TV3 | Test đúng exception/code, lỗi không bị trả success | Chưa làm |
| B10 | Socket Client–Server, nhiều client — bắt buộc | 9 bắt đầu, 10 hoàn thiện; 11–12 tích hợp | TV3 | Test Socket thật, session, disconnect, nhiều GUI | Chưa làm |
| B11 | Serialization/File I/O/DAO — bắt buộc | 9 bắt đầu, 11–12 hoàn thiện | TV4 | Restart đủ account/item/auction/bid/winner/status | Chưa làm |
| B12 | Lỗi dữ liệu/file/mạng và nhất quán khi lưu — bắt buộc xử lý lỗi; chiến lược transaction là thiết kế nhóm | 8–12; regression14 | Chủ module, TV4 tổng hợp | File hỏng không bị đè, save lỗi không báo success | Chưa làm |
| B13 | JavaFX Hello World/SceneBuilder — bước tự học trong PDF | 6 | Cả4, TV4 hỗ trợ | Mỗi người tự chạy và sửa control thử | Chưa làm |
| B14 | MVC/FXML, Controller–Model–DAO — bắt buộc | 8; rà soát10–12 | Cả4, TV3 tích hợp | UI không chứa business/đọc file trực tiếp | Chưa làm |
| B15 | Login/Register, List, Detail, Realtime bidding, Seller GUI — bắt buộc | 7 prototype; 10 màn hình chính; UX13–14 | TV4/TV3/TV2/TV1 | Chạy qua server thật, không treo, lỗi rõ | Chưa làm |
| B16 | Maven/Checkstyle/coding convention — bắt buộc | 9; chạy liên tục về sau | TV4 | Build và style check pass từ máy/clone khác | Chưa làm |
| B17 | JUnit và coverage≥60% — bắt buộc | 8 test; 10 đạt coverage; giữ11–14 | Từng chủ module, TV4 tổng hợp | Report mới, phạm vi/counter công bố rõ | Chưa làm |
| B18 | GitHub Actions build/test khi push, CI xanh — bắt buộc | 9 setup; 11–12 ổn định; giữ14 | TV4 | URL run xanh thật theo revision | Chưa làm |
| B19 | E2E GUI↔Logic↔Network↔Data, nhiều client/edge cases — bắt buộc | 11–12; regression13–14 | TV3+cả nhóm | Biên bản E2E, lỗi đã sửa, trạng thái chưa test ghi rõ | Chưa làm |
| B20 | README đầy đủ trên GitHub — bắt buộc | Khởi tạo6; hoàn thiện13–14 | Cả4, TV3 ghép | Clone/build/run/use theo README được | Chưa làm |
| B21 | Main/dev, commit thường xuyên, PR/review — quy trình trong đề | 6–15 | Cả nhóm | Lịch sử Git và review thật | Chưa làm |
| B22 | Slide, demo Server+nhiều Client, mọi người hiểu code, đóng góp/điểm thật — bắt buộc | Chuẩn bị14; trình bày15 | Cả nhóm | Slide+demo+vấn đáp+PR/commit thật | Chưa làm |
| O01 | Auto-bidding — tùy chọn | 13–14 nếu ổn định | TV1, TV2 review | Max/increment/priority/concurrency tests | Chưa quyết định |
| O02 | Anti-sniping — tùy chọn | 13–14 nếu ổn định | TV2, TV4 hỗ trợ test | Clock test gia hạn, event end mới, không bid sai | Chưa quyết định |
| O03 | Bid History Visualization LineChart — tùy chọn | 13–14 nếu ổn định | TV3, TV4 hỗ trợ test | Chart khớp history, không duplicate, FX thread đúng | Chưa quyết định |

Chỉ ghi “Đã test đạt” khi có code và bằng chứng. Không coi có file YAML là CI xanh, có FXML là GUI đã chạy, hay có stub DAO là Serialization hoàn thiện. Nếu một mốc trễ, ghi lỗi/owner/ảnh hưởng và ưu tiên xử lý phần bắt buộc, không sửa bảng để che việc chưa hoàn thành.

## 16. Tự học và câu hỏi kiểm tra theo từng tuần

JavaFX/SceneBuilder, Socket và Serialization là nội dung tự học bắt buộc trong PDF. Các liên kết là điểm bắt đầu được đề dẫn, không phải yêu cầu sao chép nguyên dự án mẫu. Chọn ví dụ phù hợp phiên bản JDK/dependencies nhóm đã thống nhất; repo dùng Kotlin/MySQL/Gson/Azure không làm chúng thành công nghệ bắt buộc của nhóm.

| Tuần | Nội dung tự học / thực hành | Nguồn từ PDF / nội dung tham chiếu | Câu hỏi mỗi người cần trả lời |
|---|---|---|---|
| 6 | OOP, Singleton/Factory, JavaFX+SceneBuilder Hello World | Trang2; [OpenJFX](https://openjfx.io/openjfx-docs/), [Design Patterns](https://refactoring.guru/design-patterns) | Abstract/override ở đâu? Factory Method khác switch tạo object? Singleton đa luồng ra sao? |
| 7 | Concurrency/Observer, JavaFX controls Login/List | Trang3 và playlist trang2 | Vì sao kiểm tra giá trong lock? Event phát lúc nào? Lost update là gì? |
| 8 | JUnit, SOLID vừa đủ, FXML/MVC | Trang3; [JUnit guide trong đề](https://docs.junit.org/5.5.0/user-guide/), [MVC JavaFX](https://www.pragmaticcoding.ca/javafx/MVC_In_JavaFX) | Test now==end thế nào? Vì sao controller không quyết định bid hợp lệ? |
| 9 | Socket/ObjectStream, Serialization, Maven/Checkstyle/Actions | Trang4; [Socket tutorial trong đề](https://www.baeldung.com/a-guide-to-java-sockets) | Vì sao flush output header trước input? Load file hỏng làm gì? |
| 10 | Multi-client, Observer qua Socket, FX thread, coverage | Trang4, ví dụ JavaFX+Socket trong đề | Event xen response xử lý sao? requestId dùng làm gì? Coverage đo gì? |
| 11 | Debug tích hợp, DTO/DAO mapping, E2E thật | Trang4–5, code và test nhóm | Đi từ nút bid đến file và sang GUI khác qua những class nào? |
| 12 | Edge cases, recovery/restart, CI và concurrency regression | Trang5, lỗi tìm thấy từ W11 | Timeout có chắc chưa commit? Save lỗi mà RAM đổi gây hậu quả gì? |
| 13 | UX, nâng cao nếu được chọn, Clock/PriorityQueue/LineChart | Trang5, LineChart video trong PDF | Tùy chọn có đi qua invariant cũ? Vì sao không sửa giá/end ở client? |
| 14 | Bàn giao/reproducible build, backup/demo, đọc module khác | Trang5–7, README và report nhóm | Người khác clone có chạy được? Report thuộc revision nào? Điều gì chưa test? |
| 15 | Thuyết trình và vấn đáp toàn bộ code | Trang5/7, nguồn thực tế nhóm viết | Tự giải thích và sửa một chức năng ngoài module mình được không? |

## 17. Mẫu cập nhật tiến độ và đóng góp

Mỗi tuần từng thành viên điền dòng riêng theo mẫu bên dưới; không ghi phân công dự kiến thành đóng góp đã làm. Đầu tuần chốt việc, giữa tuần ghép/demo nhỏ, cuối tuần review checklist W6–W15. Không dùng lại kết quả kiểm thử của dự án trước reset.

| Tuần học kỳ | Thành viên | Việc đã tự code / sửa | PR / commit | Test đã chạy và kết quả | Việc còn thiếu / người hỗ trợ | Reviewer |
|---|---|---|---|---|---|---|
| 6 | TV1 / TV2 / TV3 / TV4, tách thành4 dòng khi cập nhật | Chưa bắt đầu | Chưa có | Chưa chạy | Theo W6 | Theo bảng phân công |
| 7 | Mỗi người bổ sung dòng riêng | Chưa bắt đầu | Chưa có | Chưa chạy | Theo W7 | Theo bảng phân công |
| 8 | Mỗi người bổ sung dòng riêng | Chưa bắt đầu | Chưa có | Chưa chạy | Theo W8 | Theo bảng phân công |
| 9 | Mỗi người bổ sung dòng riêng | Chưa bắt đầu | Chưa có | Chưa chạy | Theo W9 | Theo bảng phân công |
| 10 | Mỗi người bổ sung dòng riêng | Chưa bắt đầu | Chưa có | Chưa chạy | Theo W10 | Theo bảng phân công |
| 11 | Mỗi người bổ sung dòng riêng | Chưa bắt đầu | Chưa có | Chưa chạy | Theo W11 | Theo bảng phân công |
| 12 | Mỗi người bổ sung dòng riêng | Chưa bắt đầu | Chưa có | Chưa chạy | Theo W12 | Theo bảng phân công |
| 13 | Mỗi người bổ sung dòng riêng | Chưa bắt đầu | Chưa có | Chưa chạy | Theo W13 | Theo bảng phân công |
| 14 | Mỗi người bổ sung dòng riêng | Chưa bắt đầu | Chưa có | Chưa chạy | Theo W14 | Theo bảng phân công |
| 15 | Mỗi người bổ sung dòng riêng | Chưa bắt đầu | Chưa có | Chưa chạy | Theo W15 | Theo bảng phân công |

Nếu nhóm chưa biết ngày học tương ứng, giữ nguyên số tuần học kỳ. Chỉ bổ sung ngày cụ thể khi có lịch chính thức; không tạo deadline riêng hoặc ép lịch backend tách khỏi lộ trình PDF.
