package vn.uet.auction.exception;

/** Phiên chưa mở hoặc đã ngừng nhận bid. */
public final class AuctionClosedException extends IllegalStateException {
    private static final long serialVersionUID = 1L;

    public AuctionClosedException(String message) {
        super(message);
    }
}
