# UET Online Auction — Khung để nhóm tự code

Dự án được dựng lại theo [kế hoạch tuần6–15](KE_HOACH_BTL_THEO_PDF.md) và PDF gốc trong thư mục.

**Chưa có ứng dụng hoặc test chạy được.** File Java chỉ chứa ghi chú mục đích, người phụ trách và tuần thực hiện; không có package/class/interface/enum/field/constructor/method hay logic viết sẵn. FXML/CSS/cấu hình runtime chờ nhóm tự hoàn thiện. Chưa có kết quả kiểm thử hoặc coverage.

## Bắt đầu

1. Cả nhóm đọc mục2–3 và tuần6 trong kế hoạch, chốt hợp đồng ở [api-contract](docs/api-contract.md).
2. Mở [danh mục file](docs/file-map.md), tìm file phụ trách. Tự viết package đúng đường dẫn rồi khai báo class/interface/enum, dữ liệu và hành vi.
3. TV1 bắt đầu User/Item/Factory; TV2 bắt đầu Auction/Bid/AuctionManager; TV3 bắt đầu DTO/protocol; TV4 hoàn thiện Maven, StateStore và môi trường JavaFX/JUnit.
4. Mỗi người tự viết tests tương ứng; cập nhật [tiến độ](docs/team-progress.md) và [checklist kiểm thử](docs/testing-checklist.md).
5. Tự khởi tạo Git/GitHub main/dev theo kế hoạch, review PR trước merge. Chưa tạo Git hoặc commit thay nhóm.

## Phân công

| Thành viên | Nguồn chính | GUI | Kiểm thử |
|---|---|---|---|
| TV1 | User, Item, Factory, UserService/ItemService | Seller, Admin | user/item |
| TV2 | Auction/Bid, Singleton, bid, Observer, scheduler | Auction | auction/concurrency |
| TV3 | DTO/protocol, server Socket, client SDK | Main/List/Detail | network/integration |
| TV4 | StateStore/Serialization, password utility, Maven/CI | ClientApp, Login/Register | persistence/security; tổng hợp chất lượng |

## Cấu hình hiện có

- pom.xml: Maven metadata và lựa chọn JDK17/UTF-8 ban đầu. Dependencies JavaFX/JUnit và plugins build/test/quality do TV4 tự bổ sung theo kế hoạch. Chưa có cấu hình JavaFX mainClass.
- checkstyle.xml: chỉ có root cấu hình; nhóm tự viết rules và tích hợp Maven ở tuần9.
- .github/workflows/ci.yml.example: ghi chú công việc CI, chưa kích hoạt Actions; nhóm viết YAML rồi đổi thành ci.yml ở tuần9.
- .gitignore và .editorconfig: quy tắc bỏ qua dữ liệu/build/IDE và định dạng cơ bản.
- data/.gitkeep: giữ thư mục dữ liệu rỗng; snapshot runtime không commit.
- src/main/resources/view: file FXML chờ tự thiết kế layout/controller và CSS.
- docs/diagrams: vị trí nhóm tự vẽ class diagram/kiến trúc Mermaid.

Chỉ dùng mvn test / mvn verify / chạy JavaFX sau khi nhóm đã hoàn thiện các phần tương ứng. Chưa có lệnh chạy server/client vì ServerMain và ClientApp chưa được viết.

Không tạo sẵn module auto-bidding/anti-sniping/LineChart: nhóm quyết định phạm vi tùy chọn ở tuần13–14 theo PDF.
