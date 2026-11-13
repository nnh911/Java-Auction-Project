package vn.uet.auction.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.client.ClientApp;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private ComboBox<Role> roleBox;
    @FXML private Label messageLabel;

    private ClientApp gateway;
    private Runnable navigate;

    public void configure(ClientApp gateway, Runnable navigate) {
        this.gateway = gateway;
        this.navigate = navigate;
    }

    @FXML
    private void initialize() {
        roleBox.getItems().addAll(Role.BIDDER, Role.SELLER);
        roleBox.setValue(Role.BIDDER);
    }

    @FXML
    private void submit() {
        String password = passwordField.getText();
        boolean matched = password.equals(confirmField.getText());
        passwordField.clear();
        confirmField.clear();
        if (!matched) {
            messageLabel.setText("Hai mật khẩu chưa giống nhau.");
            return;
        }
        messageLabel.getScene().getRoot().setDisable(true);
        gateway.register(usernameField.getText(), password, roleBox.getValue())
                .whenComplete((user, error) -> Platform.runLater(() -> {
                    messageLabel.getScene().getRoot().setDisable(false);
                    messageLabel.setText(error == null ? "Đã đăng ký " + user.username()
                            : error.getCause().getMessage());
                }));
    }

    @FXML
    private void navigate() {
        navigate.run();
    }
}
