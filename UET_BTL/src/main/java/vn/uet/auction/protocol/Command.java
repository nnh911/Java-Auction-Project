// FILE KHUNG — CHƯA TRIỂN KHAI.
// Phụ trách: Thành viên 3. Tuần học kỳ: 6–10.
// Package dự kiến: vn.uet.auction.protocol
// Mục đích: Nhóm tự định nghĩa enum command theo mục 3.4 trong kế hoạch.
// TODO: Nhóm tự viết package, khai báo kiểu, field, constructor và method.
// Thống nhất hợp đồng trước khi code; xem KE_HOACH_BTL_THEO_PDF.md.
package vn.uet.auction.protocol;

public enum Command {
    REGISTER,
    LOGIN,
    LIST_USERS,
    SET_USER_ACTIVE,
    CREATE_ITEM,
    LIST_ITEMS,
    GET_ITEM,
    UPDATE_ITEM,
    DELETE_ITEM,
    CREATE_AUCTION,
    LIST_AUCTIONS,
    GET_AUCTION,
    PLACE_BID,
    CONFIRM_PAYMENT,
    CANCEL_AUCTION
}
