# Danh mục file khung và người phụ trách

Trên nhánh `nht6`, 14 file TV1 trong `model/user`, `model/item`, `factory` đã triển khai tuần 6; `ItemFactoryTest` có kiểm tra console. Xem [bàn giao TV1](tv1-week6-handover.md). Các file còn lại là khung TODO. Các tuần là tuần học kỳ theo PDF; bảng bên dưới mô tả phân công dự kiến.

| File | Chủ chính | Tuần | Nội dung nhóm tự viết |
|---|---|---|---|
| [User.java](../src/main/java/vn/uet/auction/model/user/User.java) | TV1 | 6 | Lớp trừu tượng người dùng; xác định field private và hành vi chung. |
| [Bidder.java](../src/main/java/vn/uet/auction/model/user/Bidder.java) | TV1 | 6–7 | Kế thừa User; vai trò người tham gia đấu giá. |
| [Seller.java](../src/main/java/vn/uet/auction/model/user/Seller.java) | TV1 | 6–7 | Kế thừa User; vai trò quản lý sản phẩm và phiên của mình. |
| [Admin.java](../src/main/java/vn/uet/auction/model/user/Admin.java) | TV1 | 6–7 | Kế thừa User; quyền quản lý đã được nhóm thống nhất. |
| [Role.java](../src/main/java/vn/uet/auction/model/user/Role.java) | TV1 | 6 | Tự định nghĩa enum vai trò theo hợp đồng. |
| [Item.java](../src/main/java/vn/uet/auction/model/item/Item.java) | TV1 | 6 | Lớp trừu tượng sản phẩm; xác định dữ liệu chung và đóng gói. |
| [Electronics.java](../src/main/java/vn/uet/auction/model/item/Electronics.java) | TV1 | 6 | Kế thừa Item cho sản phẩm điện tử. |
| [Art.java](../src/main/java/vn/uet/auction/model/item/Art.java) | TV1 | 6 | Kế thừa Item cho sản phẩm nghệ thuật. |
| [Vehicle.java](../src/main/java/vn/uet/auction/model/item/Vehicle.java) | TV1 | 6 | Kế thừa Item cho phương tiện. |
| [ItemType.java](../src/main/java/vn/uet/auction/model/item/ItemType.java) | TV1 | 6 | Tự định nghĩa enum loại sản phẩm. |
| [Auction.java](../src/main/java/vn/uet/auction/model/auction/Auction.java) | TV2 | 6–7 | Mô hình phiên: thời gian, giá, trạng thái, lịch sử và người thắng. |
| [Bid.java](../src/main/java/vn/uet/auction/model/auction/Bid.java) | TV2 | 6–7 | Mô hình một lần đặt giá; thống nhất tính bất biến. |
| [AuctionStatus.java](../src/main/java/vn/uet/auction/model/auction/AuctionStatus.java) | TV2 | 6–7 | Tự định nghĩa enum trạng thái theo PDF. |
| [ItemCreator.java](../src/main/java/vn/uet/auction/factory/ItemCreator.java) | TV1 | 6 | Thiết kế Factory Method; chưa có phương thức hoặc logic tạo sẵn. |
| [ElectronicsCreator.java](../src/main/java/vn/uet/auction/factory/ElectronicsCreator.java) | TV1 | 6 | Concrete creator tạo Electronics theo hợp đồng nhóm. |
| [ArtCreator.java](../src/main/java/vn/uet/auction/factory/ArtCreator.java) | TV1 | 6 | Concrete creator tạo Art theo hợp đồng nhóm. |
| [VehicleCreator.java](../src/main/java/vn/uet/auction/factory/VehicleCreator.java) | TV1 | 6 | Concrete creator tạo Vehicle theo hợp đồng nhóm. |
| [UserService.java](../src/main/java/vn/uet/auction/service/UserService.java) | TV1 | 7–12 | Đăng ký, đăng nhập, role, quản lý tài khoản; không trả hash qua DTO. |
| [ItemService.java](../src/main/java/vn/uet/auction/service/ItemService.java) | TV1 | 7–12 | CRUD sản phẩm, kiểm tra chủ sở hữu và sản phẩm đã gắn phiên. |
| [AuctionService.java](../src/main/java/vn/uet/auction/service/AuctionService.java) | TV2 | 7–12 | Tạo phiên, đặt giá, vòng đời, winner, payment mô phỏng và cancel. |
| [AuctionManager.java](../src/main/java/vn/uet/auction/service/AuctionManager.java) | TV2 | 6–12 | Singleton và điều phối nghiệp vụ; nhóm tự thiết kế lock/transaction. |
| [DomainException.java](../src/main/java/vn/uet/auction/exception/DomainException.java) | TV2 | 8 | Nhóm quyết định lớp lỗi nghiệp vụ chung và hợp đồng error code. |
| [InvalidBidException.java](../src/main/java/vn/uet/auction/exception/InvalidBidException.java) | TV2 | 8 | Ngoại lệ đặt giá không hợp lệ theo yêu cầu PDF. |
| [AuctionClosedException.java](../src/main/java/vn/uet/auction/exception/AuctionClosedException.java) | TV2 | 8 | Ngoại lệ phiên không nhận bid theo yêu cầu PDF. |
| [AuthenticationException.java](../src/main/java/vn/uet/auction/exception/AuthenticationException.java) | TV1 | 8 | Ngoại lệ xác thực theo yêu cầu PDF. |
| [AuctionListener.java](../src/main/java/vn/uet/auction/observer/AuctionListener.java) | TV2 | 7–10 | Nhóm tự khai báo interface Observer và hợp đồng callback. |
| [AuctionEvent.java](../src/main/java/vn/uet/auction/observer/AuctionEvent.java) | TV2 | 7–10 | Sự kiện nghiệp vụ; không chứa mật khẩu hoặc dữ liệu nhạy cảm. |
| [AuctionScheduler.java](../src/main/java/vn/uet/auction/scheduler/AuctionScheduler.java) | TV2 | 9–12 | Tự chuyển trạng thái theo giờ server; quản lý vòng đời scheduler. |
| [ApplicationState.java](../src/main/java/vn/uet/auction/persistence/ApplicationState.java) | TV4 | 6–12 | Aggregate users/items/auctions/bids; thống nhất schema với TV1 và TV2. |
| [StateStore.java](../src/main/java/vn/uet/auction/persistence/StateStore.java) | TV4 | 6–9 | Nhóm tự khai báo interface DAO load/save và hợp đồng IOException. |
| [FileStateStore.java](../src/main/java/vn/uet/auction/persistence/FileStateStore.java) | TV4 | 9–12 | Serialization/File I/O; bảo vệ snapshot cũ và xử lý file hỏng. |
| [PasswordHasher.java](../src/main/java/vn/uet/auction/security/PasswordHasher.java) | TV4 | 6–9 | Nhóm tự triển khai hash có salt và verify; không lưu password thô. |
| [Request.java](../src/main/java/vn/uet/auction/protocol/Request.java) | TV3 | 6–10 | DTO request: nhóm thống nhất requestId, command và payload. |
| [Response.java](../src/main/java/vn/uet/auction/protocol/Response.java) | TV3 | 6–10 | DTO response: kết quả/lỗi và requestId tương ứng. |
| [EventMessage.java](../src/main/java/vn/uet/auction/protocol/EventMessage.java) | TV3 | 6–10 | DTO sự kiện gửi qua Socket; dữ liệu an toàn cho client. |
| [Command.java](../src/main/java/vn/uet/auction/protocol/Command.java) | TV3 | 6–10 | Nhóm tự định nghĩa enum command theo mục 3.4 trong kế hoạch. |
| [ErrorCode.java](../src/main/java/vn/uet/auction/protocol/ErrorCode.java) | TV3 | 6–10 | Nhóm thống nhất mã lỗi để client/server xử lý nhất quán. |
| [UserSummary.java](../src/main/java/vn/uet/auction/protocol/dto/UserSummary.java) | TV3 | 6–10 | Thông tin user an toàn; phối hợp TV1, không có password/hash. |
| [ItemSummary.java](../src/main/java/vn/uet/auction/protocol/dto/ItemSummary.java) | TV3 | 6–10 | Dữ liệu danh sách sản phẩm; phối hợp TV1. |
| [ItemDetail.java](../src/main/java/vn/uet/auction/protocol/dto/ItemDetail.java) | TV3 | 6–10 | Dữ liệu chi tiết sản phẩm; phối hợp TV1. |
| [AuctionSummary.java](../src/main/java/vn/uet/auction/protocol/dto/AuctionSummary.java) | TV3 | 6–10 | Dữ liệu danh sách phiên; phối hợp TV2. |
| [AuctionDetail.java](../src/main/java/vn/uet/auction/protocol/dto/AuctionDetail.java) | TV3 | 6–10 | Dữ liệu chi tiết phiên và lịch sử; phối hợp TV2. |
| [BidSummary.java](../src/main/java/vn/uet/auction/protocol/dto/BidSummary.java) | TV3 | 6–10 | Dữ liệu một bid gửi cho client; phối hợp TV2. |
| [ServerMain.java](../src/main/java/vn/uet/auction/server/ServerMain.java) | TV3 | 9–12 | Điểm chạy server; nhóm tự viết main, đọc cấu hình và shutdown. |
| [AuctionServer.java](../src/main/java/vn/uet/auction/server/AuctionServer.java) | TV3 | 9–12 | Server Socket phục vụ nhiều client; giới hạn và quản lý tài nguyên. |
| [ClientHandler.java](../src/main/java/vn/uet/auction/server/ClientHandler.java) | TV3 | 9–12 | Nhận request, dùng session server và gửi response/event. |
| [Session.java](../src/main/java/vn/uet/auction/server/Session.java) | TV3 | 9–12 | Danh tính sau login, không tin actor do client tự khai. |
| [CommandDispatcher.java](../src/main/java/vn/uet/auction/server/CommandDispatcher.java) | TV3 | 7–12 | Ánh xạ command tới service; phối hợp TV1 và TV2. |
| [ClientApp.java](../src/main/java/vn/uet/auction/client/ClientApp.java) | TV4 | 6–10 | Điểm chạy JavaFX; nhóm tự viết Application, start và main. |
| [AuctionClient.java](../src/main/java/vn/uet/auction/client/network/AuctionClient.java) | TV3 | 9–12 | Client SDK Socket: gửi request, nhận response/event, timeout và close. |
| [ClientEventListener.java](../src/main/java/vn/uet/auction/client/network/ClientEventListener.java) | TV3 | 9–10 | Nhóm tự khai báo callback sự kiện mạng phía client. |
| [ClientState.java](../src/main/java/vn/uet/auction/client/model/ClientState.java) | TV3 | 7–12 | Dữ liệu/model phía GUI, không giữ entity sống phía server. |
| [LoginController.java](../src/main/java/vn/uet/auction/client/controller/LoginController.java) | TV4 | 7–10 | Controller đăng nhập; logic xác thực thuộc server. |
| [RegisterController.java](../src/main/java/vn/uet/auction/client/controller/RegisterController.java) | TV4 | 7–10 | Controller đăng ký; không tự cấp role Admin. |
| [MainController.java](../src/main/java/vn/uet/auction/client/controller/MainController.java) | TV3 | 8–10 | Điều hướng màn hình và session phía GUI. |
| [AuctionListController.java](../src/main/java/vn/uet/auction/client/controller/AuctionListController.java) | TV3 | 7–10 | Danh sách phiên, refresh và giữ lựa chọn. |
| [ItemDetailController.java](../src/main/java/vn/uet/auction/client/controller/ItemDetailController.java) | TV3 | 8–10 | Hiển thị chi tiết từ DTO qua client SDK. |
| [SellerController.java](../src/main/java/vn/uet/auction/client/controller/SellerController.java) | TV1 | 7–10 | Form CRUD sản phẩm và tạo phiên, dùng API của server. |
| [AuctionController.java](../src/main/java/vn/uet/auction/client/controller/AuctionController.java) | TV2 | 7–10 | Đấu giá trực tiếp, trạng thái/winner và cập nhật trên FX thread. |
| [AdminController.java](../src/main/java/vn/uet/auction/client/controller/AdminController.java) | TV1 | 8–12 | Màn quản lý tài khoản theo quy tắc nhóm đã chốt. |

## File kiểm thử

| File | Chủ chính | Tuần | Kịch bản cần tự viết |
|---|---|---|---|
| [UserServiceTest.java](../src/test/java/vn/uet/auction/user/UserServiceTest.java) | TV1 | 8–12 | Đăng ký/login đúng-sai, trùng tên, role, tài khoản khóa và dữ liệu rỗng. |
| [ItemServiceTest.java](../src/test/java/vn/uet/auction/item/ItemServiceTest.java) | TV1 | 8–12 | CRUD, chủ sở hữu, Item đã có phiên, tên/mô tả không hợp lệ. |
| [ItemFactoryTest.java](../src/test/java/vn/uet/auction/item/ItemFactoryTest.java) | TV1 | 6–8 | Factory tạo đúng ba subtype; giải thích đa hình. |
| [AuctionServiceTest.java](../src/test/java/vn/uet/auction/auction/AuctionServiceTest.java) | TV2 | 8–12 | Tạo phiên, bid đúng/sai, payment/cancel, snapshot kết quả. |
| [AuctionLifecycleTest.java](../src/test/java/vn/uet/auction/auction/AuctionLifecycleTest.java) | TV2 | 8–12 | Clock tại start/end, không bid, winner, chuyển trạng thái sai. |
| [ConcurrentBiddingTest.java](../src/test/java/vn/uet/auction/auction/ConcurrentBiddingTest.java) | TV2 | 7–14 | Nhiều luồng/client, giá cuối/history nhất quán; không chỉ dựa vào sleep. |
| [ProtocolContractTest.java](../src/test/java/vn/uet/auction/network/ProtocolContractTest.java) | TV3 | 8–10 | Command/payload/DTO/errorCode/requestId theo hợp đồng. |
| [AuctionServerTest.java](../src/test/java/vn/uet/auction/network/AuctionServerTest.java) | TV3 | 9–12 | Socket thật, session/role, nhiều client, ngắt kết nối. |
| [RealtimeUpdateTest.java](../src/test/java/vn/uet/auction/network/RealtimeUpdateTest.java) | TV3 | 10–12 | Client khác nhận bid/state event không refresh thủ công. |
| [StateStoreContractTest.java](../src/test/java/vn/uet/auction/persistence/StateStoreContractTest.java) | TV4 | 8–12 | Hợp đồng load/save, bản sao dữ liệu, fake save failure. |
| [FileStateStoreTest.java](../src/test/java/vn/uet/auction/persistence/FileStateStoreTest.java) | TV4 | 9–12 | File thật round-trip/restart; file hỏng/thiếu/lỗi ghi; giữ snapshot cũ. |
| [PasswordHasherTest.java](../src/test/java/vn/uet/auction/security/PasswordHasherTest.java) | TV4 | 7–12 | Hash có salt khác nhau, verify đúng/sai; không ghi password vào log. |
| [AuctionEndToEndTest.java](../src/test/java/vn/uet/auction/integration/AuctionEndToEndTest.java) | TV3 | 11–14 | GUI/network/service/file thật, nhiều client và restart; phối hợp cả4. |

## GUI và cấu hình

FXML/CSS/application.properties là file chờ nhóm tự điền. Tên view tương ứng controller trong client/controller; fx:controller, fx:id và handlers chưa được khai báo. TV4 chủ trì cấu hình Maven, Checkstyle và CI, review cùng nhóm. File ci.yml.example chỉ đổi thành ci.yml sau khi viết workflow hoàn chỉnh ở tuần9.
