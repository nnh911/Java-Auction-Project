package vn.uet.auction.item;

import java.util.List;
import java.util.UUID;
import vn.uet.auction.factory.ArtCreator;
import vn.uet.auction.factory.ElectronicsCreator;
import vn.uet.auction.factory.ItemCreator;
import vn.uet.auction.factory.VehicleCreator;
import vn.uet.auction.model.item.Art;
import vn.uet.auction.model.item.Electronics;
import vn.uet.auction.model.item.Item;
import vn.uet.auction.model.item.ItemType;
import vn.uet.auction.model.item.Vehicle;
import vn.uet.auction.model.user.Admin;
import vn.uet.auction.model.user.Bidder;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.model.user.Seller;
import vn.uet.auction.model.user.User;

/** Week-6 console checks; run with scripts/test-week6-tv1.ps1, not a JUnit test yet. */
public final class ItemFactoryTest {
    private static int checks;

    private ItemFactoryTest() {
    }

    public static void main(String[] args) {
        UUID sellerId = UUID.randomUUID();
        User seller = new Seller(sellerId, "  nam_hoang  ");
        List<User> users = List.of(
                new Bidder(UUID.randomUUID(), "bidder_01"),
                seller,
                new Admin(UUID.randomUUID(), "admin_01"));
        List<Role> roles = List.of(Role.BIDDER, Role.SELLER, Role.ADMIN);
        for (int i = 0; i < users.size(); i++) {
            check(users.get(i).getRole() == roles.get(i), "wrong role");
        }
        check(seller.getId().equals(sellerId), "seller id changed");
        check(seller.getUsername().equals("nam_hoang"), "username not trimmed");

        List<ItemCreator> creators = List.of(
                new ElectronicsCreator(), new ArtCreator(), new VehicleCreator());
        List<ItemType> types = List.of(
                ItemType.ELECTRONICS, ItemType.ART, ItemType.VEHICLE);
        List<Class<? extends Item>> classes = List.of(
                Electronics.class, Art.class, Vehicle.class);
        for (int i = 0; i < creators.size(); i++) {
            ItemCreator creator = creators.get(i);
            UUID itemId = UUID.randomUUID();
            Item item = creator.createItem(itemId, sellerId, "  San pham  ", "  Mo ta  ");
            check(classes.get(i).isInstance(item), "factory returned wrong subtype");
            check(item.getType() == types.get(i), "wrong item type");
            check(item.getId().equals(itemId), "item id changed");
            check(item.getSellerId().equals(sellerId), "owner changed");
            check(item.getName().equals("San pham"), "name not trimmed");
            check(item.getDescription().equals("Mo ta"), "description not trimmed");
            check(creator.createItem(UUID.randomUUID(), sellerId, "A", "")
                    .getDescription().isEmpty(), "empty description should be allowed");
            expect(IllegalArgumentException.class,
                    () -> creator.createItem(UUID.randomUUID(), sellerId, "   ", ""));
            expect(IllegalArgumentException.class,
                    () -> creator.createItem(UUID.randomUUID(), sellerId, "x".repeat(121), ""));
            expect(IllegalArgumentException.class,
                    () -> creator.createItem(UUID.randomUUID(), sellerId, "A", "x".repeat(2001)));
            expect(NullPointerException.class,
                    () -> creator.createItem(null, sellerId, "A", ""));
            expect(NullPointerException.class,
                    () -> creator.createItem(UUID.randomUUID(), null, "A", ""));
            expect(NullPointerException.class,
                    () -> creator.createItem(UUID.randomUUID(), sellerId, null, ""));
            expect(NullPointerException.class,
                    () -> creator.createItem(UUID.randomUUID(), sellerId, "A", null));
            System.out.println(item.getType() + " | " + item.getName());
        }

        expect(IllegalArgumentException.class, () -> new Seller(sellerId, "   "));
        expect(IllegalArgumentException.class, () -> new Seller(sellerId, "ab"));
        expect(IllegalArgumentException.class, () -> new Seller(sellerId, "a".repeat(31)));
        expect(IllegalArgumentException.class, () -> new Seller(sellerId, "a b"));
        expect(NullPointerException.class, () -> new Seller(null, "seller_01"));
        expect(NullPointerException.class, () -> new Seller(sellerId, null));
        check(new Seller(sellerId, "abc").getUsername().length() == 3, "minimum username");
        check(new Seller(sellerId, "a".repeat(30)).getUsername().length() == 30,
                "maximum username");
        Item boundary = new ArtCreator().createItem(
                UUID.randomUUID(), sellerId, "x".repeat(120), "x".repeat(2000));
        check(boundary.getName().length() == 120, "maximum name");
        check(boundary.getDescription().length() == 2000, "maximum description");
        System.out.println("PASS: " + checks + " checks");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException exception) {
            if (type.isInstance(exception)) {
                checks++;
                return;
            }
            throw new AssertionError("Wrong exception: " + exception, exception);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
