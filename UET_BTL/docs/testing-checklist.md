# Checklist kiểm thử

TV4 điều phối; từng người phụ trách test module mình. TV1 có kiểm tra console tại ItemFactoryTest.java, chạy bằng scripts/test-week6-tv1.ps1; chưa có JUnit/test runner Maven chung hoặc coverage. Các test khác vẫn là khung.

| Nhóm kịch bản | Owner | Tuần | Bằng chứng cần điền | Trạng thái |
|---|---|---|---|---|
| User/role/login/register | TV1 | 8–12 | TODO: test và kết quả thực tế | Chưa viết |
| Item CRUD/Factory/owner | TV1 | 6–12 | TODO | Chưa viết |
| Auction/bid/vòng đời/winner | TV2 | 7–12 | TODO | Chưa viết |
| Concurrency/biên thời gian | TV2, TV4 hỗ trợ | 7–14 | TODO | Chưa viết |
| Protocol/Socket/realtime | TV3 | 8–12 | TODO | Chưa viết |
| Serialization/file hỏng/lỗi ghi/restart | TV4 | 9–12 | TODO | Chưa viết |
| Password hashing | TV4 | 7–12 | TODO | Chưa viết |
| E2E GUI↔Logic↔Network↔Data | Cả nhóm, TV3 chủ trì | 11–14 | TODO | Chưa viết |
| Checkstyle/JUnit/coverage≥60%/CI | TV4, từng chủ module | 9–14 | TODO: report và URL Actions | Chưa cấu hình hoàn chỉnh |

Dùng bảng yêu cầu tại mục15 trong [kế hoạch](../KE_HOACH_BTL_THEO_PDF.md). Chỉ cập nhật đạt sau khi tự chạy và có assertion/bằng chứng.

## Kết quả riêng TV1 tuần 6 — 2026-10-28

- `./scripts/test-week6-tv1.ps1 -JdkHome 'C:/Program Files/Java/jdk-26.0.2'`: **PASS: 57 checks**, exit code 0. Bao gồm subtype/role, giữ ID/chủ, trim, null, tên trắng, độ dài sai và biên hợp lệ.
- Compile toàn bộ file Java trong `src/main/java` với `javac -encoding UTF-8 --release 17`: đạt. Các module khác hiện chỉ là comment, kết quả này không chứng minh chúng đã hoạt động.
- Compile `examples/week6-tv1/HelloTv1.java` với JavaFX base/graphics/controls 17.0.16 có sẵn trong cache: đạt. Chưa mở cửa sổ, thử Button hoặc xác nhận FXML/SceneBuilder.
- Chưa chạy trên runtime JDK 17 thật; chưa chạy Maven/JUnit, chưa đo coverage. Chưa kiểm tra CRUD hoặc quyền owner trong service tuần 7.

Xem [bàn giao và lệnh chạy](tv1-week6-handover.md). Không đánh dấu toàn bộ Item CRUD/owner đã đạt dựa trên Factory/model.
