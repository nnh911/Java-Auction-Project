package vn.uet.auction.client.controller;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.*;
import vn.uet.auction.model.item.ItemType;
import vn.uet.auction.protocol.dto.ItemDetail;

/** Seller form backed by the injected gateway. */
public final class SellerController {
    public interface Gateway {
        List<ItemDetail> list();
        void create(ItemType type, String name, String description);
        void update(UUID id, ItemType type, String name, String description);
        void delete(UUID id);
    }
    @FXML private BorderPane root;
    @FXML private ListView<ItemDetail> items;
    @FXML private ComboBox<ItemType> type;
    @FXML private TextField name;
    @FXML private TextArea description;
    @FXML private Label message;
    private Gateway gateway;

    @FXML private void initialize() {
        type.getItems().setAll(ItemType.values());
        type.getSelectionModel().selectFirst();
        items.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(ItemDetail item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.name() + " (" + item.type() + ")");
            }
        });
        items.getSelectionModel().selectedItemProperty().addListener((o, old, item) -> {
            if (item != null) {
                name.setText(item.name()); description.setText(item.description()); type.setValue(item.type());
            }
        });
    }
    public void setGateway(Gateway gateway) {
        this.gateway = Objects.requireNonNull(gateway);
        refresh();
    }

    @FXML private void refresh() {
        run(() -> { });
    }

    @FXML private void create() {
        ItemType itemType = type.getValue();
        String itemName = name.getText();
        String itemDescription = description.getText();
        run(() -> gateway.create(itemType, itemName, itemDescription));
    }

    @FXML private void update() {
        ItemDetail item = items.getSelectionModel().getSelectedItem();
        if (item == null) {
            message.setText("Select an item first");
            return;
        }
        ItemType itemType = type.getValue();
        String itemName = name.getText();
        String itemDescription = description.getText();
        run(() -> gateway.update(item.id(), itemType, itemName, itemDescription));
    }

    @FXML private void delete() {
        ItemDetail item = items.getSelectionModel().getSelectedItem();
        if (item == null) {
            message.setText("Select an item first");
            return;
        }
        run(() -> gateway.delete(item.id()));
    }

    private void run(Runnable action) {
        root.setDisable(true);
        CompletableFuture.supplyAsync(() -> {
            action.run();
            return gateway.list();
        }).whenComplete((result, error) -> Platform.runLater(() -> {
            root.setDisable(false);
            if (error == null) {
                items.getItems().setAll(result);
                message.setText("OK");
            } else {
                message.setText(error.getCause().getMessage());
            }
        }));
    }
}
