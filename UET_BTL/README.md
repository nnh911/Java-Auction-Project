# UET Online Auction — bàn giao tuần 6

Cập nhật 2026-10-31, sau khi ghép code trên nhánh `tuần-6-chung`. Nền tuần 6 cơ bản đã có, nhóm có thể bắt đầu tuần 7; kiểm thử tích hợp, một số kiểm thử và tài liệu xác nhận vẫn còn thiếu. Đây chưa phải ứng dụng đấu giá hoàn chỉnh.

## Đọc trước khi tiếp tục

1. [Bàn giao tuần 6 và đầu việc tuần 7](docs/week6-handoff.md).
2. [Hợp đồng API hiện tại](docs/api-contract.md).
3. [Bằng chứng kiểm thử](docs/testing-checklist.md) và [tiến độ](docs/team-progress.md).
4. [Kế hoạch tuần 6–15](KE_HOACH_BTL_THEO_PDF.md) để biết yêu cầu dự kiến. Các câu “chưa triển khai” trong kế hoạch gốc mô tả thời điểm tạo khung, không phản ánh toàn bộ code hiện nay.

## Những phần đã có

| Người | Phần triển khai |
|---|---|
| TV1 — Nguyễn Nam Hoàng | User/Item hierarchy, enums, Factory Method, Serializable, kiểm tra console model/Factory |
| TV2 — Nguyễn Kiên Cường | Auction/Bid, Singleton AuctionManager, AuctionService nhận Clock/map/lock, snapshot, test model/service |
| TV3 — Lê Hải Thịnh | Request/Response/EventMessage, Command/ErrorCode, 6 DTO và test protocol |
| TV4 — Nguyễn Hoàng Minh Tuấn | ApplicationState, StateStore/MemoryStateStore, copy dữ liệu, PasswordHasher và test, Maven/JUnit |

UserService/ItemService, đặt giá thật qua service, Observer, Socket, lưu file thật và GUI chính chưa hoàn chỉnh. File có comment TODO cần được đọc phần code phía dưới trước khi kết luận là khung.

## Build và test

Mở `UET_BTL/pom.xml` trong IntelliJ và Add as Maven Project, Reload Maven. Nếu máy không có lệnh mvn, dùng cửa sổ Maven → Execute Maven Goal.

Cấu hình hiện tại đặt Java source/release 17, JUnit 6.1.3 và Surefire 3.5.4. Cần JDK tương thích với thư viện test; chưa xác nhận build trên runtime JDK 17 thật. Không đồng nhất release 17 với việc mọi dependency chạy được trên Java 17.

Tại thư mục chứa pom.xml:

```text
mvn clean test
```

Trong Execute Maven Goal chỉ nhập `clean test`. Người dùng đã báo BUILD SUCCESS sau sửa quyền truy cập Auction ngày 2026-10-31. Mỗi máy vẫn cần tự chạy lại.

Bộ console TV1 không được JUnit tự chạy: sau testCompile, chạy main của `vn.uet.auction.item.ItemFactoryTest` trong IntelliJ. Kết quả mong đợi `PASS: 57 checks`.

Không dùng lệnh script/đường dẫn examples trong README cũ: các file đó không có trong bản hiện tại. pom.xml hiện chưa có dependency/plugin JavaFX; chưa có lệnh chạy server/client/GUI chính dùng chung.

## Quy trình ghép

Mỗi người test, commit và push nhánh riêng. Fetch trước khi merge, review diff, giải quyết conflict từng đoạn, hoàn tất commit merge rồi mới merge nhánh kế tiếp. Chạy clean test sau ghép. Đặc biệt giữ cả phương thức public của Auction phục vụ service/test và copy constructor phục vụ persistence. Không chọn nguyên một phía nếu làm mất chức năng phía kia.
