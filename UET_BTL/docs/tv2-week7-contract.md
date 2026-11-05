# Hợp đồng bàn giao TV2 tuần 7 — chốt chữ ký ngày 2026-11-05

**Trạng thái: đã được người dùng/TV2 chốt để gửi nhóm.** Chưa ghi nhận TV1/TV3/TV4
đã review hoặc triển khai. Các đoạn Java dưới đây là hợp đồng tài liệu, không phải
class giả được thêm vào src/main. Phân biệt “đã chốt chữ ký” với “đã triển khai”.
Giữ nguyên API tuần 6 và các API TV2 đã triển khai; không đổi tên, thứ tự tham số.
Các mô tả cũ trong api-contract.md là lịch sử; mục này thay thế trạng thái đề xuất
của chính các chữ ký liệt kê bên dưới.

## 1. TV1 — API đọc phục vụ kiểm tra quyền

Package `vn.uet.auction.service`, dùng `java.util.UUID`,
`vn.uet.auction.model.user.User` và `vn.uet.auction.model.item.Item` hiện có:

```java
// public instance methods trong UserService
public User getUser(UUID userId);
public boolean isUserActive(UUID userId);

// public instance method trong ItemService
public Item getItem(UUID itemId);
```

**Đã chốt chữ ký; chưa triển khai.** Getter trả model bất biến hiện có, chỉ dùng nội
bộ server; không dùng User làm DTO gửi client. `getUser` trả user dù đang bị khóa;
role lấy từ `User.getRole()`. `getItem` trả Item; owner lấy từ `Item.getSellerId()`.

- ID null: `NullPointerException`; ID không tồn tại: `NoSuchElementException`.
- `isUserActive` trả false chỉ khi user tồn tại và đang bị khóa; không trả false để
  biểu diễn không tìm thấy. User tồn tại, không khóa: true.
- Không trả null hoặc model giả khi không tìm thấy. Không sửa User/Item để chứa
  JavaFX hay Socket. TV1+TV4 quyết định nơi lưu active; chữ ký trên không ép schema.
- Tra cứu user/active/Item và cập nhật phiên phải trong cùng lock dùng chung với
  thao tác khóa user, sửa/xóa Item. Khóa riêng từng getter không đủ bảo đảm tính nguyên tử.
  Không giữ lock bên ngoài rồi gọi public AuctionService, vì callback phải chạy sau unlock.
  TV2 sẽ nối kiểm tra vào thân thao tác đang giữ lock sau khi API TV1 có thật.

Actor ID phải lấy từ session/ngữ cảnh xác thực của server. Không nhận role/active
hoặc owner do client tự khai. `getUser` là API nội bộ, không quyết định visibility
của màn hình quản lý user.

## 2. TV2 — API nghiệp vụ đã triển khai

Package `vn.uet.auction.service`; các method dưới đây có cả ở AuctionService và
AuctionManager. Tiền `java.math.BigDecimal`, thời gian `java.time.Instant`:

```java
public AuctionSnapshot createAuction(UUID sellerId, UUID itemId,
        BigDecimal startingPrice, Instant start, Instant end);
public Optional<AuctionSnapshot> getAuction(UUID auctionId);
public List<AuctionSnapshot> listAuctions();
public AuctionSnapshot placeBid(UUID auctionId, UUID bidderId, BigDecimal amount);
public List<StatusChange> closeDueAuctions();
public AuctionSnapshot confirmPayment(UUID actorId, UUID auctionId);
public AuctionSnapshot cancelAuction(UUID actorId, UUID auctionId);
public void addListener(AuctionListener listener);
public void removeListener(AuctionListener listener);
```

Lưu ý thứ tự `placeBid(auctionId, bidderId, amount)` nhưng payment/cancel là
`(actorId, auctionId)`. Không đổi thứ tự để tránh caller biên dịch được nhưng chạy sai.
Constructor service vẫn là `AuctionService(Clock)` và
`AuctionService(Clock, ReentrantLock, Map<UUID, Auction>)`.

Observer nội bộ package `vn.uet.auction.observer`:

```java
public record AuctionEvent(UUID auctionId, AuctionStatus from, AuctionStatus to,
        Instant occurredAt, AuctionSnapshot snapshot, AuctionEvent.Type type) { }
// Type: BID_ACCEPTED, AUCTION_STARTED, AUCTION_FINISHED, AUCTION_CANCELED, PAYMENT_CONFIRMED
public interface AuctionListener {
    void onAuctionEvent(AuctionEvent event);
}
```

Scheduler package `vn.uet.auction.scheduler`:

```java
public interface AuctionScheduler {
    List<StatusChange> tick();
}
// Sử dụng: AuctionScheduler scheduler = service::closeDueAuctions;
```

Các record/interface đã có constructor/method thật trong code. Xem api-contract.md
cho invariant, exception tạm thời và hành vi listener. Event chụp dữ liệu tại commit,
phát sau unlock; không bảo đảm thứ tự callback toàn cục giữa các thread. Scheduler
W7 chủ động tick, chưa chạy thread tự động. Custom exception thuộc tuần 8.

## 3. TV3 — gateway bất đồng bộ cho form đấu giá

**Đã chốt chữ ký; chưa triển khai.** Interface mới do TV3 sở hữu, tách khỏi Socket để
fake phục vụ test đặt trong src/test. Package `vn.uet.auction.client.network`:

```java
public interface AuctionGateway {
    CompletionStage<AuctionDetail> getAuction(UUID auctionId);
    CompletionStage<AuctionDetail> placeBid(UUID auctionId, BigDecimal amount);
    CompletionStage<AuctionDetail> confirmPayment(UUID auctionId);
    CompletionStage<AuctionDetail> cancelAuction(UUID auctionId);
    void addEventListener(ClientEventListener listener);
    void removeEventListener(ClientEventListener listener);
}

@FunctionalInterface
public interface ClientEventListener {
    void onEvent(EventMessage event);
}
```

Imports: `java.util.concurrent.CompletionStage`, `java.util.UUID`,
`java.math.BigDecimal`, `vn.uet.auction.protocol.dto.AuctionDetail`,
`vn.uet.auction.protocol.EventMessage`. Dùng DTO đã có, không trả AuctionSnapshot
hoặc Auction mutable ra client. Gateway không nhận actorId; session server xác định actor.

- UUID/amount/listener null: ném NPE ngay khi gọi. Method request trả stage không null.
- Thành công trả AuctionDetail non-null, phản ánh trạng thái sau thao tác; mutation
  không trả void. GET không tìm thấy hoàn thành stage với lỗi NOT_FOUND.
- Lỗi server hoàn thành stage exceptionally bằng GatewayException bên dưới. Không
  trả DTO giả hay thành công rỗng. Mất kết nối: IOException; timeout: TimeoutException
  trong exceptional completion. Có thể được bọc trong CompletionException.
- Gateway không block FX thread; future/callback có thể chạy ở thread nền.
  Controller dùng Platform.runLater để render, không gọi join/get trên FX thread.
- Listener null bị từ chối; đăng ký trùng theo equals chỉ một lần, gỡ vắng mặt là no-op.
  RuntimeException của listener phải được cô lập và log. Callback event không null.
- Event là tín hiệu refresh theo auctionId. Client tuần tự hóa refresh của cùng phiên,
  gộp event phát sinh trong lúc request đang chạy thành một refresh kế tiếp; không áp
  thứ tự thời gian event để ghi đè dữ liệu. Gỡ listener khi rời màn hình.

Để truyền ErrorCode mà không phân tích message, chốt exception client (TV3), cùng package:

```java
public final class GatewayException extends RuntimeException {
    public GatewayException(ErrorCode errorCode, String message);
    public ErrorCode getErrorCode();
}
```

`ErrorCode` là `vn.uet.auction.protocol.ErrorCode` đã có. errorCode/message bắt buộc
non-null; message không trắng (IAE nếu trắng). Exception chỉ chứa mã và thông báo an
toàn, không chứa credentials/stacktrace server. Đây là chữ ký mới đã chốt trong tài
liệu, chưa thêm class production của TV3.

## 4. TV2 + TV3 — biên controller và protocol

Chốt cho TV2 triển khai tiếp (hiện AuctionController vẫn là khung):

```java
// vn.uet.auction.client.controller.AuctionController
public AuctionController();
public void configure(AuctionGateway gateway, UUID auctionId);
public void dispose();
```

FXML dùng constructor không tham số; owner navigation gọi configure sau FXMLLoader.load.
configure/dispose chạy trên FX thread. configure chỉ một lần mỗi controller; null bị
NPE, gọi lại bị ISE. dispose gỡ listener, gọi nhiều lần không lỗi; callback/request
hoàn thành sau dispose không cập nhật màn hình. Không tự đóng gateway dùng chung.
FXML `view/auction.fxml`: fx:controller=`vn.uet.auction.client.controller.AuctionController`.

| fx:id | Kiểu JavaFX | Handler onAction |
|---|---|---|
| statusLabel, currentPriceLabel, winnerLabel, messageLabel | Label | — |
| bidAmountField | TextField | #handlePlaceBid |
| bidButton | Button | #handlePlaceBid |
| refreshButton | Button | #handleRefresh |
| paymentButton | Button | #handleConfirmPayment |
| cancelButton | Button | #handleCancelAuction |

Handlers private @FXML, không phải service API. Gateway quyết định kết quả; controller
chỉ parse input, gọi gateway và render. Không coi disable nút là kiểm tra quyền server.

Chốt response GET/PLACE_BID/CONFIRM_PAYMENT/CANCEL_AUCTION là AuctionDetail. CREATE_AUCTION
cũng trả AuctionDetail sau mapping từ snapshot. Chưa cần thay Response.ok vì data có thật.
Payload GET/CONFIRM_PAYMENT/CANCEL_AUCTION chốt thêm nested record trong Request theo
cấu trúc payload hiện có: `AuctionIdPayload(UUID auctionId) implements Payload`;
UUID non-null, dùng chung cho ba command, dispatcher vẫn kiểm tra command/payload.
LIST_AUCTIONS dùng `EmptyPayload() implements Payload`, result
`ArrayList<AuctionSummary>` (Serializable), không giả định List interface Serializable.
TV3 cập nhật sealed permits/test khi triển khai, TV2 không sửa Request trong đợt này.

Snapshot start/end -> DTO startTime/endTime; bids -> history qua BidSummary; winnerId
nullable như record hiện có. Thêm PAYMENT_CONFIRMED vào EventMessage.EventType là
thay đổi TV3 phải thực hiện cùng mapper và test; giữ nguyên constructor EventMessage.
Các event khác giữ tên, không serialize AuctionEvent nội bộ ra mạng.

Không map mọi IllegalArgumentException thành INVALID_BID: cần dựa vào nhánh lỗi/ngữ
cảnh đã xác định. NOT_FOUND: ID không tồn tại; FORBIDDEN: sai quyền/active=false;
INVALID_BID: giá sai; AUCTION_CLOSED: phiên không nhận bid; CONFLICT: chuyển trạng
thái không hợp lệ hoặc Item đã có phiên; INVALID_REQUEST: null/payload/parse sai;
UNAUTHENTICATED: chưa login; INTERNAL_ERROR: lỗi ngoài dự kiến. TV3+TV2 bổ sung lỗi
nghiệp vụ có loại phân biệt ở tuần 8, không phân tích chuỗi thông báo.

## 5. TV4 — State và JavaFX

**Đã chốt hướng tích hợp:** một ApplicationState và một ReentrantLock cho tất cả
kiểm tra/cập nhật liên quan; snapshot copy dưới cùng lock. Auction.history là nguồn
bid chuẩn; State.bids là chỉ mục dẫn xuất, không có đường ghi độc lập. Khi nối State,
TV2+TV4 đồng bộ chỉ mục trong cùng thao tác nhận bid và dựng lại/kiểm tra từ history
khi load. Hiện code chỉ ghi history; chưa ghi nhận wiring/chỉ mục đã triển khai.
Cơ chế load/init Singleton và rollback khi save lỗi thuộc đợt tích hợp sau, chưa
thay chữ ký getInstance hoặc constructor service trong tài liệu này.

**Đã triển khai pom theo yêu cầu rõ ràng của người dùng:** JavaFX Controls/FXML 17.0.21,
javafx-maven-plugin 0.0.8, mainClass `vn.uet.auction.client.ClientApp`. Giữ Java release 17,
không hard-code classifier OS; Maven resolve native libs theo nền tảng.
ClientApp vẫn là file khung do TV4 triển khai, nên chưa có GUI để chạy/kiểm chứng.
Sau khi có ClientApp thật: `mvn -f UET_BTL/pom.xml javafx:run`.

Tham khảo cấu hình: [OpenJFX Maven](https://openjfx.io/openjfx-docs/maven),
[JavaFX 17 release notes](https://gluonhq.com/products/javafx/openjfx-17-release-notes/).

## 6. Phần chưa được quyết định bởi việc chốt chữ ký

Giới hạn tiền/scale/bước tăng, chính sách tài khoản khóa được payment/cancel hay không,
đấu giá lại sau FINISHED/PAID, schema lưu active và rollback save lỗi vẫn cần quyết
định nghiệp vụ riêng; không tự coi các chữ ký này là phê duyệt các quy tắc đó.
Backend hiện dùng quy tắc giá tăng và chặn trùng Item trừ CANCELED đã ghi trong baseline.
Quyền Admin cancel là mục tiêu tích hợp sau API TV1, hiện chỉ owner cancel đã triển khai.

Gửi TV1 mục 1; TV3 mục 3–4; TV4 mục 5; TV2 giữ mục 2 và triển khai controller theo mục 4.
Không cần đổi API đã chốt để bắt đầu các phần việc này.

## 7. Kết quả kiểm tra ngày 2026-11-05

- `mvn -f UET_BTL/pom.xml test`: BUILD SUCCESS, compile main/test release 17;
  80 tests, 0 failure/error/skipped, chạy trên JDK 25.
- `mvn -f UET_BTL/pom.xml dependency:tree -Dincludes=org.openjfx`: BUILD SUCCESS;
  Controls/FXML cùng Graphics/Base và native Linux đều resolve phiên bản 17.0.21.
- Đã kiểm tra các JAR JavaFX Linux đọc được và plugin 0.0.8 có goal run.
- `git diff --check`: sạch. Chưa chạy GUI, FXMLLoader hay javafx:run vì ClientApp
  và các màn hình hiện còn khung. Chưa kiểm chứng runtime JDK 17 hoặc OS khác.
- Lượt Maven đầu bị chặn ghi cache ~/.m2; đã chạy lại có cấp quyền và kiểm tra thành công.

Thay đổi đợt chốt này: tài liệu này (mới), link trạng thái trong api-contract.md,
và pom.xml (ngoại lệ phạm vi TV4 được người dùng yêu cầu rõ ràng). Không triển khai
UserService/ItemService/gateway/DTO của các thành viên khác trong đợt chốt hợp đồng.
