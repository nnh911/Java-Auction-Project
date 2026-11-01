# Tiến độ thực tế sau ghép tuần 6

Cập nhật 2026-10-31. Nguồn: code trên tuần-6-chung và kết quả được báo trong phiên làm việc. Không thay cho xác nhận đóng góp cá nhân/review của từng người.

| Người | Có trong code chung | Còn lại / bằng chứng |
|---|---|---|
| TV1 — Nguyễn Nam Hoàng | User/Item/Factory, Serializable, ItemFactoryTest console | 57 checks từng chạy đạt; JavaFX tự chạy được theo người dùng. Cần review/API, chạy lại khi code đổi |
| TV2 — Nguyễn Kiên Cường | Auction/Bid/status, Manager Singleton, Service Clock/lock/map, snapshot, AuctionTest/AuctionServiceTest | Chưa thấy test Singleton; placeBid là stub tuần 7; chốt State và quyền; JavaFX chưa có bằng chứng |
| TV3 — Lê Hải Thịnh | Protocol, DTO, ProtocolContractTest | Ảnh máy Thịnh cho thấy fake login 1 test đạt, nhưng FakeGatewayTest chưa có trong nhánh chung lúc cập nhật; thiếu mapping/tài liệu xác nhận và luồng thử đầy đủ |
| TV4 — Nguyễn Hoàng Minh Tuấn | StateStore, ApplicationState, MemoryStateStore copy, PasswordHasher và 3 test, Maven | StateStoreContractTest còn khung; thiếu JavaFX/FXML và xác nhận môi trường các máy |

Người dùng báo BUILD SUCCESS sau sửa quyền truy cập Auction. Commit merge không chứng minh mọi nghiệp vụ đã chạy; xem [checklist](testing-checklist.md). Chưa ghi nhận review chéo chính thức. Nhóm bắt đầu tuần 7 và hoàn thiện các mục tồn tại trong [bàn giao](week6-handoff.md).

AI đã hỗ trợ TV1 và sửa visibility Auction trong phiên làm việc; từng thành viên cần đọc và giải thích code mình bàn giao. Không tự chuyển bảng phân công thành bằng chứng tác giả.
