# Hợp đồng module — nhóm tự hoàn thiện

Chủ trì TV3; TV1/TV2/TV4 cùng review. Chốt ban đầu tuần6, cập nhật có kiểm soát trước khi tích hợp. Tham khảo mục3 trong [kế hoạch](../KE_HOACH_BTL_THEO_PDF.md).

Chưa có chữ ký Java hoặc DTO đã triển khai. Tên file khung là vị trí dự kiến, không phải hợp đồng đã được thực hiện.

| Module/API | Chữ ký được cả nhóm duyệt | Input/output và null | Quyền/precondition | Exception/error code | Owner/reviewer | Trạng thái |
|---|---|---|---|---|---|---|
| User | TODO | TODO | TODO | TODO | TV1/TV2 | Chưa chốt |
| Item | TODO | TODO | TODO | TODO | TV1/TV2 | Chưa chốt |
| Auction | TODO | TODO | TODO | TODO | TV2/TV3 | Chưa chốt |
| Protocol/Socket | TODO | TODO | TODO | TODO | TV3/TV4 | Chưa chốt |
| StateStore | TODO | TODO | TODO | TODO | TV4/TV1 | Chưa chốt |

## Quyết định cần ghi lại

- TODO: Kiểu tiền, giới hạn giá, quy tắc giá đầu/bước giá.
- TODO: Thời gian server, định dạng DTO, winner chưa có.
- TODO: Quyền Admin, điều kiện sửa/xóa Item, payment/cancel.
- TODO: Request/response/event schema, requestId, actor từ session.
- TODO: Serialization schema, lock/transaction, lỗi save và khôi phục.
- TODO: Chủ sở hữu file chung và cách báo thay đổi hợp đồng.
