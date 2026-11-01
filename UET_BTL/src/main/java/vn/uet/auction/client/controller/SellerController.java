package vn.uet.auction.client.controller;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import vn.uet.auction.model.item.ItemType;
import vn.uet.auction.protocol.dto.ItemDetail;

/** Week-7 prototype: injected fake gateway, no domain rules in the controller. */
public final class SellerController {
    public interface Gateway {
        List<ItemDetail> list();
        void create(ItemType type, String name, String description);
        void update(UUID id, ItemType type, String name, String description);
        void delete(UUID id);
    }
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
    public void setGateway(Gateway gateway) { this.gateway = Objects.requireNonNull(gateway); refresh(); }
    @FXML private void refresh() { run(() -> items.getItems().setAll(gateway.list())); }
    @FXML private void create() { run(() -> { gateway.create(type.getValue(), name.getText(), description.getText()); reload(); }); }
    @FXML private void update() { run(() -> { gateway.update(selected(), type.getValue(), name.getText(), description.getText()); reload(); }); }
    @FXML private void delete() { run(() -> { gateway.delete(selected()); reload(); }); }
    private UUID selected() {
        ItemDetail item = items.getSelectionModel().getSelectedItem();
        if (item == null) throw new IllegalArgumentException("Select an item first");
        return item.id();
    }
    private void reload() { items.getItems().setAll(gateway.list()); }
    private void run(Runnable action) {
        if (gateway == null) { message.setText("Gateway not configured"); return; }
        try { action.run(); message.setText("OK - in-memory prototype"); }
        catch (IllegalArgumentException | IllegalStateException | SecurityException e) { message.setText(e.getMessage()); }
    }
}
