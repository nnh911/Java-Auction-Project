package vn.uet.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.client.ClientApp.FakeAuthGateway;

public class RegisterController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;
    @FXML private ComboBox<Role> roleBox;
    @FXML private Label messageLabel;

    private FakeAuthGateway gateway;
    private Runnable navigate;

    public void configure(FakeAuthGateway gateway, Runnable navigate) {
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
        try {
            String password = passwordField.getText();
            if (!password.equals(confirmField.getText())) {
                messageLabel.setText("Hai mật khẩu chưa giống nhau.");
                return;
            }
            messageLabel.setText(gateway.register(usernameField.getText(), password, roleBox.getValue()));
        } catch (IllegalArgumentException exception) {
            messageLabel.setText(exception.getMessage());
        } finally {
            passwordField.clear();
            confirmField.clear();
        }
    }

    @FXML
    private void navigate() {
        navigate.run();
    }
}
