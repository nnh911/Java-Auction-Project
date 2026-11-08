package vn.uet.auction.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import vn.uet.auction.client.controller.SellerController;
import vn.uet.auction.model.item.ItemType;
import vn.uet.auction.protocol.dto.ItemDetail;

/** Independent sample-data launcher; deliberately not wired into TV4's ClientApp. */
public final class SellerPrototype extends Application {
    @Override public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/seller.fxml"));
        Scene scene = new Scene(loader.load());
        SellerController controller = loader.getController();
        controller.setGateway(new SampleGateway());
        stage.setTitle("Seller prototype - TV1 week 7"); stage.setScene(scene); stage.show();
    }
    private static final class SampleGateway implements SellerController.Gateway {
        private final UUID seller = UUID.randomUUID();
        private final List<ItemDetail> items = new ArrayList<>();
        SampleGateway() { create(ItemType.ART, "Sample painting", "Sample data only"); }
        public List<ItemDetail> list() { return List.copyOf(items); }
        public void create(ItemType type, String name, String description) {
            items.add(new ItemDetail(UUID.randomUUID(), seller, name, description, type));
        }
        public void update(UUID id, ItemType type, String name, String description) {
            int index = index(id); items.set(index, new ItemDetail(id, seller, name, description, type));
        }
        public void delete(UUID id) { items.remove(index(id)); }
        private int index(UUID id) {
            for (int i = 0; i < items.size(); i++) if (items.get(i).id().equals(id)) return i;
            throw new IllegalArgumentException("Item not found");
        }
    }
    public static void main(String[] args) { launch(args); }
}
