package vn.uet.auction.client;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import vn.uet.auction.client.controller.SellerController;
import vn.uet.auction.model.item.ItemType;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.persistence.ApplicationState;
import vn.uet.auction.protocol.dto.ItemDetail;
import vn.uet.auction.security.PasswordHasher;
import vn.uet.auction.service.ItemService;
import vn.uet.auction.service.UserService;

/** Standalone Seller form backed by UserService and ItemService in memory. */
public final class SellerPrototype extends Application {
    private SampleGateway gateway;

    @Override public void init() {
        gateway = new SampleGateway();
    }

    @Override public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/seller.fxml"));
        Scene scene = new Scene(loader.load());
        SellerController controller = loader.getController();
        controller.setGateway(gateway);
        stage.setTitle("Seller prototype - TV1 week 8");
        stage.setScene(scene);
        stage.show();
    }
    private static final class SampleGateway implements SellerController.Gateway {
        private final UUID seller;
        private final ItemService items;

        SampleGateway() {
            var state = new ApplicationState();
            var lock = new ReentrantLock();
            var users = new UserService(state, lock, new PasswordHasher());
            seller = users.register("seller_demo", "Demo123", Role.SELLER).id();
            items = new ItemService(state, lock);
            create(ItemType.ART, "Sample painting", "Sample data only");
        }

        public List<ItemDetail> list() {
            return items.listItems(seller).stream()
                    .filter(item -> item.sellerId().equals(seller))
                    .map(item -> items.getItem(seller, item.id()).orElseThrow())
                    .toList();
        }

        public void create(ItemType type, String name, String description) {
            items.createItem(seller, type, name, description);
        }
        public void update(UUID id, ItemType type, String name, String description) {
            items.updateItem(seller, id, type, name, description);
        }

        public void delete(UUID id) {
            items.deleteItem(seller, id);
        }
    }
    public static void main(String[] args) { launch(args); }
}
