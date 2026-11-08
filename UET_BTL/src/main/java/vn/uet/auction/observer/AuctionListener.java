package vn.uet.auction.observer;

@FunctionalInterface
public interface AuctionListener {
    void onAuctionEvent(AuctionEvent event);
}
