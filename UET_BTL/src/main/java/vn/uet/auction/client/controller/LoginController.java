package vn.uet.auction.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import vn.uet.auction.client.ClientApp;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private ClientApp gateway;
    private Runnable navigate;

    public void configure(ClientApp gateway, Runnable navigate) {
        this.gateway = gateway;
        this.navigate = navigate;
    }

    @FXML
    private void submit() {
        var request = gateway.login(usernameField.getText(), passwordField.getText());
        passwordField.clear();
        messageLabel.getScene().getRoot().setDisable(true);
        request.whenComplete((user, error) -> Platform.runLater(() -> {
            messageLabel.getScene().getRoot().setDisable(false);
            if (error != null) {
                messageLabel.setText(error.getCause().getMessage());
            } else if (!gateway.openSeller(user)) {
                messageLabel.setText("Đăng nhập thành công: " + user.username());
            }
        }));
    }

    @FXML
    private void navigate() {
        navigate.run();
    }
}
