# Bàn giao tuần 6 → tuần 7

Cập nhật 2026-10-31, kiểm tra trên tuần-6-chung, revision code trước cập nhật tài liệu: 148da45. Đây là trạng thái quan sát được và hướng dẫn tiếp tục, không phải biên bản cả bốn thành viên đã ký duyệt.

## Trạng thái

Nền model/Factory/Singleton/protocol/memory store đã ghép. Người dùng báo Maven BUILD SUCCESS sau sửa visibility Auction. Được bắt đầu tuần 7 nhưng không ghi tuần 6 hoàn tất 100%.

## Lưu ý cho AI/thành viên tiếp tục

- Đọc api-contract.md và code thật trước khi thay chữ ký. Kế hoạch, file-map và comment khung cũ không phải bằng chứng chức năng chưa có hoặc đã xong.
- Không triển khai lại User/Item/Factory, AuctionService hoặc PasswordHasher từ đầu.
- Không khẳng định có xác thực, phân quyền, Socket, lưu file, GUI hoặc đặt giá service thật chỉ vì đã có tên class/command.
- Auction.placeBid không tồn tại; AuctionService.placeBid hiện ném UnsupportedOperationException. Test xác nhận stub ném lỗi không chứng minh bid hoạt động.
- Auction.refreshStatus/acceptBid/markPaid/cancel là public để khác package gọi; giữ kiểm tra invariant. Quyền và lock do service kiểm soát.
- Giữ Auction(Auction other) khi merge: ApplicationState copy constructor dùng nó.
- MemoryStateStore tạo bản sao qua constructor, không dùng Java Serialization. User/Item Serializable là chuẩn bị cho lưu trữ sau này; ApplicationState hiện chưa Serializable.
- AuctionManager hiện tạo service/map/lock riêng; chưa nối tự động với ApplicationState chung. Không tạo thêm State song song rồi gọi đó là tích hợp.
- Snapshot nội bộ không phải DTO mạng; TV3 cần mapping rõ.
- Không tự ghi review/chữ ký xác nhận, test hay CI đạt khi chưa có bằng chứng.

## Việc còn lại đầu tuần 7

| Chủ | Việc |
|---|---|
| TV1 | Chạy lại console 57 checks; phối hợp TV4 thiết kế chỗ lưu hash/active; viết UserService/ItemService theo kế hoạch |
| TV2 | Test Singleton; giữ Clock và lock cho thao tác đọc-kiểm tra-ghi; triển khai placeBid/quyền/vòng đời/Observer; thống nhất wiring State với TV4 |
| TV3 | Đưa FakeGatewayTest đã demo trên máy Thịnh vào nhánh chung; mở rộng thử hợp đồng; map AuctionSnapshot sang DTO; chốt schema với nhóm |
| TV4 | Viết StateStoreContractTest cho memory load/save và tính độc lập; xác nhận cấu hình JDK/JUnit; bổ sung cấu hình JavaFX/FXML thử nghiệm |
| Cả nhóm | Test tích hợp model → State → service → DTO → memory save/load; cập nhật review và kết quả thực chạy |

FXML/JavaFX mới xác nhận Hoàng đã tự chạy ví dụ qua trao đổi; chưa xác nhận cả bốn máy/SceneBuilder. Socket/file thật/CI/coverage thuộc các mốc sau, không coi là thiếu code tuần 6 bắt buộc phải làm ngay.

## Các quyết định cần cả nhóm xác nhận

- Cách truyền actor và tra role/active, chỗ giữ password hash.
- Wiring một State/lock chung; chiến lược bids map so với history Auction để không lệch hai bản dữ liệu.
- Có cho đấu giá lại sau FINISHED hay chỉ CANCELED; code hiện tại chỉ loại CANCELED khỏi kiểm tra trùng Item.
- Quyền cancel/payment và việc sửa/xóa Item đã có phiên theo đề xuất kế hoạch.
- Giới hạn tiền/bước giá; hiện code mới kiểm tra giá dương và model nhận bid lớn hơn currentPrice.
- Kiểu dữ liệu trả về command, error mapping và payload cho các command chưa được hỗ trợ.
