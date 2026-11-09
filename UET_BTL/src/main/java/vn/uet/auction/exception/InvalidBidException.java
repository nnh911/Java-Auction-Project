package vn.uet.auction.exception;

import vn.uet.auction.service.AuctionService.Rejection;

/** Giá đặt không thỏa quy tắc của phiên đang nhận bid. */
public final class InvalidBidException extends Rejection {
    private static final long serialVersionUID = 1L;

    public InvalidBidException(String message) {
        super(Reason.INVALID_BID, message);
    }
}
