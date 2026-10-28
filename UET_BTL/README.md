# UET Online Auction — Khung để nhóm tự code

Dự án được dựng lại theo [kế hoạch tuần6–15](KE_HOACH_BTL_THEO_PDF.md) và PDF gốc trong thư mục.

**Nhánh nht6 có phần TV1 tuần 6:** User/Item hierarchy, Factory Method và kiểm tra console. Xem cách chạy ở cuối README. Có ví dụ JavaFX Label/Button riêng trong `examples/week6-tv1`. Các module khác, FXML và cấu hình Maven/JUnit/JavaFX chung vẫn là khung; chưa có ứng dụng đấu giá hoàn chỉnh hoặc coverage.

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

## TV1 tuần 6

14 file model/Factory và sơ đồ đã triển khai. API và validation được ghi tại [api-contract](docs/api-contract.md), chờ nhóm review. CRUD, login/register, Serialization và GUI Seller thuộc phần tiếp theo.

Mở PowerShell tại `UET_BTL` để kiểm tra (thay đường dẫn JDK nếu cần):

```powershell
./scripts/test-week6-tv1.ps1 -JdkHome 'C:/Program Files/Java/jdk-26.0.2'
```

Kết quả mong đợi: `PASS: 57 checks`. Script biên dịch lại với `--release 17`, kiểm tra model/Factory trong thư mục tạm sạch. Đây là kiểm tra console, chưa tự chạy qua `mvn test`. Kết quả và giới hạn kiểm chứng ghi tại [testing-checklist](docs/testing-checklist.md).

Ví dụ JavaFX độc lập (dùng các JAR Windows đã có trong cache máy Hoàng):

```powershell
$jdk = 'C:/Program Files/Java/jdk-26.0.2'
$version = '17.0.16'
$fxCache = Join-Path $env:USERPROFILE '.m2/repository/org/openjfx'
$fxJars = @('base', 'graphics', 'controls') | ForEach-Object {
    Join-Path $fxCache "javafx-$_/$version/javafx-$_-$version-win.jar"
}
$fxPath = $fxJars -join [System.IO.Path]::PathSeparator
New-Item -ItemType Directory -Force target/tv1-hello | Out-Null
& "$jdk/bin/javac.exe" -encoding UTF-8 --release 17 --module-path $fxPath --add-modules javafx.controls -d target/tv1-hello examples/week6-tv1/HelloTv1.java
if ($LASTEXITCODE -ne 0) { throw 'JavaFX compilation failed' }
& "$jdk/bin/java.exe" --module-path $fxPath --add-modules javafx.controls -cp target/tv1-hello vn.uet.auction.examples.HelloTv1
```

Cửa sổ hiện tên Hoàng; bấm Button để đổi Label. Máy khác cần chỉnh đường dẫn JDK/JavaFX tương ứng. Ví dụ này chưa thay thế phần FXML/SceneBuilder chung của TV4.
