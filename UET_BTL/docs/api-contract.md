# Hợp đồng module — nhóm tự hoàn thiện

Chủ trì TV3; TV1/TV2/TV4 cùng review. Chốt ban đầu tuần6, cập nhật có kiểm soát trước khi tích hợp. Tham khảo mục3 trong [kế hoạch](../KE_HOACH_BTL_THEO_PDF.md).

TV1 đã triển khai model User/Item và Factory tuần 6 trên nhánh `nht6`; chữ ký bên dưới là bản triển khai chờ nhóm review. Các module khác và DTO vẫn là khung. Không coi phần đã triển khai là đã được cả nhóm duyệt.

| Module/API | Chữ ký được cả nhóm duyệt | Input/output và null | Quyền/precondition | Exception/error code | Owner/reviewer | Trạng thái |
|---|---|---|---|---|---|---|
| User | TODO | TODO | TODO | TODO | TV1/TV2 | Chưa chốt |
| Item | TODO | TODO | TODO | TODO | TV1/TV2 | Chưa chốt |
| Auction | TODO | TODO | TODO | TODO | TV2/TV3 | Chưa chốt |
| Protocol/Socket | TODO | TODO | TODO | TODO | TV3/TV4 | Chưa chốt |
| StateStore | TODO | TODO | TODO | TODO | TV4/TV1 | Chưa chốt |

## API TV1 tuần 6 — đã triển khai, chờ review

Package gốc `vn.uet.auction`. Không thay chữ ký bên dưới riêng lẻ khi module khác đã sử dụng; cập nhật caller/test cùng PR nếu cần đổi.

| Package / kiểu | Constructor / public methods |
|---|---|
| `model.user.Role` | Enum `BIDDER`, `SELLER`, `ADMIN` |
| `model.user.User` (abstract) | Constructor protected `(UUID id, String username)`; `UUID getId()`, `String getUsername()`, abstract `Role getRole()` |
| `model.user.Bidder`, `Seller`, `Admin` | Constructor public `(UUID id, String username)`; override `getRole()` theo lớp con |
| `model.item.ItemType` | Enum `ELECTRONICS`, `ART`, `VEHICLE` |
| `model.item.Item` (abstract) | Constructor protected `(UUID id, UUID sellerId, String name, String description)`; `UUID getId()`, `UUID getSellerId()`, `String getName()`, `String getDescription()`, abstract `ItemType getType()` |
| `model.item.Electronics`, `Art`, `Vehicle` | Constructor public cùng tham số Item; override `getType()` theo lớp con |
| `factory.ItemCreator` (abstract) | `Item createItem(UUID id, UUID sellerId, String name, String description)` |
| `factory.ElectronicsCreator`, `ArtCreator`, `VehicleCreator` | Override `createItem(...)`, trả subtype tương ứng (covariant return type) |

Quy tắc đã code (giới hạn độ dài là lựa chọn triển khai cần nhóm review):

- Các ID dùng `java.util.UUID`, do caller cấp; model/factory không tự thay ID.
- Mọi tham số constructor không được null; vi phạm ném `NullPointerException`.
- Username được `strip()`; sau đó khớp `[A-Za-z0-9_]{3,30}`. Chưa xử lý trùng tên hoặc phân biệt hoa/thường trong service.
- Tên Item được `strip()`, dài 1–120 ký tự; mô tả được `strip()`, dài 0–2000 ký tự. Sai giá trị ném `IllegalArgumentException`.
- Getters trả dữ liệu không null. Model bất biến, field private final, không có setter. Role/type do subtype quyết định.
- `sellerId` chỉ là tham chiếu định danh. Xác minh tài khoản Seller, quyền sửa/xóa và Item đã gắn phiên là việc service tuần 7.
- Chưa có active/password/hash hoặc Serializable trong model này. TV4/TV1 phối hợp thiết kế ở tuần tiếp theo; không truyền entity chứa hash qua DTO.
- Admin constructor không thay thế kiểm tra quyền. Public register tuần 7 phải chặn tự cấp ADMIN theo đề xuất kế hoạch.

Nguồn chạy và hướng dẫn bàn giao: [TV1 W6](tv1-week6-handover.md). Reviewer dự kiến TV2; chưa ghi nhận review thật.

## Quyết định cần ghi lại

- TODO: Kiểu tiền, giới hạn giá, quy tắc giá đầu/bước giá.
- TODO: Thời gian server, định dạng DTO, winner chưa có.
- TODO: Quyền Admin, điều kiện sửa/xóa Item, payment/cancel.
- TODO: Request/response/event schema, requestId, actor từ session.
- TODO: Serialization schema, lock/transaction, lỗi save và khôi phục.
- TODO: Chủ sở hữu file chung và cách báo thay đổi hợp đồng.
