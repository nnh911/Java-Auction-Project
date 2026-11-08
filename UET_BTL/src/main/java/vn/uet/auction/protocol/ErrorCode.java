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
