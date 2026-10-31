// FILE KHUNG — CHƯA TRIỂN KHAI.
// Phụ trách: Thành viên 4. Tuần học kỳ: 6–12.
// Package dự kiến: vn.uet.auction.persistence
// Mục đích: Aggregate users/items/auctions/bids; thống nhất schema với TV1 và TV2.
// TODO: Nhóm tự viết package, khai báo kiểu, field, constructor và method.
// Thống nhất hợp đồng trước khi code; xem KE_HOACH_BTL_THEO_PDF.md.
package vn.uet.auction.persistence;

import vn.uet.auction.model.user.User;
import vn.uet.auction.model.item.Item;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ApplicationState {

    private Map<UUID, User> users;
    private Map<UUID, Item> items;

    public ApplicationState() {
        users = new HashMap<>();
        items = new HashMap<>();
    }

    public Map<UUID, User> getUsers() {
        return users;
    }

    public Map<UUID, Item> getItems() {
        return items;
    }
}