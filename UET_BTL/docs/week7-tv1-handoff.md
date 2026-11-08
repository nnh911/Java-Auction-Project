# Bàn giao TV1 - Nguyễn Nam Hoàng - tuần 7

Phạm vi: UserService register/login, quyền Admin và khóa/mở tài khoản; ItemService CRUD/owner/Item đã có phiên; Seller prototype dùng fake gateway. Không triển khai phần đấu giá/Observer/dispatcher của TV2/TV3, exception tuần 8, Socket/lưu file/CI các tuần sau.

Đọc phần bổ sung TV1 tuần 7 trong [api-contract.md](api-contract.md) để ghép API. Thêm AccountState và map accounts vào ApplicationState là thay đổi chung tối thiểu để hash/active tồn tại cùng dữ liệu và được MemoryStateStore sao chép. Cần TV4 review schema, TV2 review nghiệp vụ; chưa có chữ ký review.

Ví dụ wiring (tất cả dùng cùng đối tượng):

```java
ApplicationState state = new ApplicationState();
ReentrantLock lock = new ReentrantLock();
UserService users = new UserService(state, lock, new PasswordHasher());
ItemService items = new ItemService(state, lock);
AuctionService auctions = new AuctionService(Clock.systemUTC(), lock, state.getAuctions());
```

Admin được seed bằng bootstrapAdmin ở startup tin cậy; không gọi từ register/command public. TV3 lấy actorId từ session. TV2 giữ lock chung khi gọi requireOwnedItem và tạo phiên; AuctionService hiện có chưa được sửa để tự kiểm tra quyền. AuctionManager hiện vẫn có map riêng, phải wiring sau ở module phụ trách.

Chạy backend: `mvn test` trong UET_BTL (máy này dùng Maven bundled trong IntelliJ với `-o`). Kết quả thực chạy: 79 tests, 0 failures/errors/skips; gồm 7 tests mới TV1. Chạy lại ItemFactoryTest console tuần 6: PASS 57 checks. Test bao gồm concurrent register trùng tên, memory copy credentials/active, CRUD đủ ba loại, quyền sai, khóa tài khoản và bảo vệ Item có phiên kể cả canceled.

Seller prototype: chạy main `vn.uet.auction.client.SellerPrototype` trong IntelliJ với JavaFX 17.0.16; VM options nếu chạy bằng classpath: `--module-path <thư mục chứa các javafx-*-17.0.16-win.jar> --add-modules javafx.controls,javafx.fxml`. Dữ liệu chỉ trong RAM của fake gateway, có một painting mẫu. Create/Update/Delete/Refresh minh họa control; chưa phải CRUD qua server. Smoke test JavaFX đã chạy trên FX thread: tải FXML, inject controller, gắn fake gateway và gọi nút Create/Update/Delete đều PASS. Chưa xác nhận bố cục/thao tác GUI thủ công.

Không sửa file week6-handoff.md có sẵn chưa được Git theo dõi; không push/merge hoặc ghi nhận người khác đã review.
