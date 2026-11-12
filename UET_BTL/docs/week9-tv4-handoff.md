# TV4 bàn giao tuần 9 — Lưu snapshot và kiểm tra build

Nguyễn Hoàng Minh Tuấn (TV4) · Nhánh `tuant9` · Nền tuần 8 `f98e106`.

| Nội dung | Cách dùng | Người phối hợp |
|---|---|---|
| Lưu và tải snapshot | `new FileStateStore(path)` triển khai `StateStore`. `load()` trả State rỗng khi file chưa có. File hỏng hoặc không đọc được sẽ báo `IOException`. `save(state)` ghi file tạm cùng thư mục, sau đó thay snapshot chính bằng `ATOMIC_MOVE`. | TV2 dùng cùng State và lock khi ghép transaction. TV3 lấy đường dẫn snapshot từ cấu hình server. |
| Dữ liệu lưu | Snapshot chứa user, tài khoản, sản phẩm, phiên đấu giá, lịch sử và chỉ mục bid. Tài khoản giữ hash và trạng thái khóa. | TV1 kiểm tra đăng nhập, khóa tài khoản và CRUD sau khi khởi động lại. TV2 kiểm tra lịch sử và chỉ mục bid. |
| Maven và CI | Từ thư mục gốc chạy `mvn -f UET_BTL/pom.xml clean verify`. Checkstyle chạy ở pha `verify`. GitHub Actions dùng Temurin 25, còn POM biên dịch với `release=17`. | Cả nhóm chạy cùng lệnh trước khi ghép. Sau khi push, kiểm tra mục Actions và tải artifact `maven-reports`. |
| Login/Register | Form hiện dùng gateway mẫu. | TV3 bàn giao API SDK cho `LOGIN` và `REGISTER`. TV4 nối controller, chạy lời gọi mạng ngoài JavaFX Application Thread rồi cập nhật giao diện trên luồng JavaFX. |

## Thống nhất khi ghép với TV1, TV2 và TV3

1. **TV2 và TV4** dùng một lock để sao chép State, áp dụng thay đổi lên bản sao và gọi `save`. Chỉ cập nhật State đang chạy sau khi lưu thành công. Phát sự kiện sau khi nhả lock. Nếu lưu lỗi thì giữ State cũ và không phát sự kiện thành công.
2. **TV1 và TV2** kiểm tra mọi thao tác ghi đi qua cơ chế lưu chung. Giữ `Auction.history` và chỉ mục bid nhất quán trong cùng snapshot.
3. **TV3 và TV4** thống nhất đường dẫn snapshot và thời điểm gọi `load()` khi khởi động server. Nếu file hỏng, báo lỗi khởi động và giữ file để kiểm tra.
4. **Cả nhóm** chạy lại build trên cùng commit đã ghép. Kiểm tra đăng ký, đăng nhập, tạo sản phẩm, tạo phiên, đặt bid rồi khởi động lại server và đọc lại dữ liệu. Thử thêm trường hợp `save` lỗi để kiểm tra RAM và sự kiện.

## Phạm vi kiểm thử

Trên bản làm việc `tuant9`, Maven `clean verify` chạy thành công với **118 test đạt**, 0 test bỏ qua và **0 vi phạm Checkstyle**. Môi trường kiểm tra dùng JDK 25.0.4.1 và Maven 3.9.16.

Bốn test `FileStateStoreTest` kiểm tra file chưa tồn tại, lưu và mở lại bằng store mới, file hỏng, và lỗi thay file đích kèm dọn file tạm. Kiểm thử restart server qua Socket và lượt chạy Actions trên GitHub được xác nhận sau khi ghép và push.

Báo cáo JUnit nằm ở `UET_BTL/target/surefire-reports/`. Báo cáo JaCoCo nằm ở `UET_BTL/target/site/jacoco/index.html`.
