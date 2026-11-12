package vn.uet.auction.persistence;

import vn.uet.auction.model.user.User;
import vn.uet.auction.model.item.Item;
import vn.uet.auction.model.auction.Auction;
import vn.uet.auction.model.auction.Bid;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ApplicationState implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<UUID, User> users;
    private Map<UUID, Item> items;
    private Map<UUID, Auction> auctions;
    private Map<UUID, Bid> bids;
    private Map<UUID, AccountState> accounts;

    public ApplicationState() {
        users = new HashMap<>();
        items = new HashMap<>();
        auctions = new HashMap<>();
        bids = new HashMap<>();
        accounts = new HashMap<>();
    }

    /** Internal only; access under the shared application lock. */
    public Map<UUID, AccountState> getAccounts() { return accounts; }

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
        this.accounts = new HashMap<>(other.accounts);

        for (Map.Entry<UUID, Auction> entry : other.auctions.entrySet()) {
            this.auctions.put(entry.getKey(), new Auction(entry.getValue()));
        }
    }
}
