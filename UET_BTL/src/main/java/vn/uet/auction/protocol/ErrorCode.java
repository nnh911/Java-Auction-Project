// FILE KHUNG — CHƯA TRIỂN KHAI.
// Phụ trách: Thành viên 3. Tuần học kỳ: 6–10.
// Package dự kiến: vn.uet.auction.protocol
// Mục đích: Nhóm thống nhất mã lỗi để client/server xử lý nhất quán.
// TODO: Nhóm tự viết package, khai báo kiểu, field, constructor và method.
// Thống nhất hợp đồng trước khi code; xem KE_HOACH_BTL_THEO_PDF.md.
package vn.uet.auction.protocol;

// ĐỀ XUẤT: cần TV1/TV2/TV4 review trước khi dùng chung.
public enum ErrorCode {
    INVALID_REQUEST,
    UNSUPPORTED_COMMAND,
    AUTHENTICATION_FAILED,
    UNAUTHENTICATED,
    FORBIDDEN,
    NOT_FOUND,
    CONFLICT,
    INVALID_BID,
    AUCTION_CLOSED,
    INTERNAL_ERROR
}
