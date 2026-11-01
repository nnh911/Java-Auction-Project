# Kiểm thử sau ghép tuần 6

Cập nhật 2026-10-31. Chỉ ghi đạt theo phạm vi thực sự được chạy.

| Bộ kiểm tra | Trạng thái / phạm vi |
|---|---|
| Maven build/test chung | Người dùng báo BUILD SUCCESS sau sửa visibility Auction; báo cáo trong target có AuctionTest 13, AuctionServiceTest 13, PasswordHasherTest 3 không lỗi. Không dùng tổng này thay số test đầy đủ (protocol có nested tests) |
| ProtocolContractTest | Trước đó đã chạy 43 test đạt; cần chạy lại cùng revision mới bằng clean test |
| ItemFactoryTest | Console main, trước đó PASS: 57 checks; không tự chạy bằng JUnit/Maven |
| FakeGatewayTest | Ảnh máy Thịnh: 1 test login giả đạt; file chưa có trong nhánh chung lúc cập nhật |
| StateStoreContractTest | File khung; chưa có test memory store |
| Singleton identity | Chưa thấy test |
| Tích hợp bốn module | Chưa có test chạy chung model → service → DTO → memory store |
| JavaFX | Hoàng báo chạy Label/Button thành công; chưa xác nhận cả bốn máy, FXML/SceneBuilder |
| Socket/file thật/concurrency/CI/coverage | Chưa nghiệm thu; làm theo mốc kế hoạch |

## Cách chạy

Trong thư mục UET_BTL: mvn clean test. Trong IntelliJ Maven nhập goal clean test.
Chạy riêng main vn.uet.auction.item.ItemFactoryTest sau biên dịch test, cần PASS: 57 checks.
Không dùng scripts/test-week6-tv1.ps1 vì script không có trong bản hiện tại.

## Test cần bổ sung

- Memory store: round-trip users/items/auctions/bids; sửa State gốc sau save không ảnh hưởng store; sửa State tải ra và Auction bên trong không ảnh hưởng lần load tiếp; hai store độc lập; null save bị từ chối.
- Singleton: assertSame hai lần getInstance, không dùng Singleton để chia sẻ dữ liệu giữa test nghiệp vụ.
- Tích hợp: tạo Seller/Item qua Factory, đưa vào State, tạo AuctionService(Clock cố định, lock chung, state.getAuctions()), tạo phiên, map snapshot sang AuctionDetail/Response, save/load và đối chiếu ID/giá/thời gian/trạng thái.
- Chưa kiểm tra bid thật xuyên service ở tuần 6 vì placeBid vẫn ném UnsupportedOperationException.

Lưu số test, JDK, revision và lỗi thực tế khi chạy. release 17 chỉ là target biên dịch; cấu hình JUnit 6 cần xác nhận runtime phù hợp. Không ghi JDK 17/cả bốn máy đã đạt nếu chưa chạy.
