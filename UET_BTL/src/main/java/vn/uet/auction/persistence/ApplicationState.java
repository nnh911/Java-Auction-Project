// FILE KHUNG — CHƯA TRIỂN KHAI.
// Phụ trách: Thành viên 4. Tuần học kỳ: 6–12.
// Package dự kiến: vn.uet.auction.persistence
// Mục đích: Aggregate users/items/auctions/bids; thống nhất schema với TV1 và TV2.
// TODO: Nhóm tự viết package, khai báo kiểu, field, constructor và method.
// Thống nhất hợp đồng trước khi code; xem KE_HOACH_BTL_THEO_PDF.md.
package vn.uet.auction.persistence;

import vn.uet.auction.model.user.User;
import vn.uet.auction.model.item.Item;
import vn.uet.auction.model.auction.Auction;
import vn.uet.auction.model.auction.Bid;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ApplicationState {

    private Map<UUID, User> users;
    private Map<UUID, Item> items;
    private Map<UUID, Auction> auctions;
    private Map<UUID, Bid> bids;

    public ApplicationState() {
        users = new HashMap<>();
        items = new HashMap<>();
        auctions = new HashMap<>();
        bids = new HashMap<>();
    }

    public Map<UUID, User> getUsers() {
        return users;
    }

    public Map<UUID, Item> getItems() {
        return items;
    }

    public Map<UUID, Auction> getAuctions() {
        return auctions;
    }

    public Map<UUID, Bid> getBids() {
        return bids;
    }
    public ApplicationState(ApplicationState other) {
        if (other == null) {
            throw new IllegalArgumentException("State must not be null");
        }

        this.users = new HashMap<>(other.users);
        this.items = new HashMap<>(other.items);
        this.auctions = new HashMap<>();
        this.bids = new HashMap<>(other.bids);

        for (Map.Entry<UUID, Auction> entry : other.auctions.entrySet()) {
            this.auctions.put(entry.getKey(), new Auction(entry.getValue()));
        }
    }
}
