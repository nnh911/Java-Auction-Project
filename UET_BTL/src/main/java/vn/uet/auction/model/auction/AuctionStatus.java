package vn.uet.auction.model.auction;
/** Vòng đời phiên đấu giá: OPEN -> RUNNING -> FINISHED -> PAID, hoặc CANCELED. */
public enum AuctionStatus {
    OPEN,
    RUNNING,
    FINISHED,
    PAID,
    CANCELED
}