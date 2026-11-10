package vn.uet.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import vn.uet.auction.client.ClientApp.FakeAuthGateway;

public class LoginController {
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    private FakeAuthGateway gateway;
    private Runnable navigate;

    public void configure(FakeAuthGateway gateway, Runnable navigate) {
        this.gateway = gateway;
        this.navigate = navigate;
    }

    @FXML
    private void submit() {
        try {
            messageLabel.setText(gateway.login(usernameField.getText(), passwordField.getText()));
        } catch (IllegalArgumentException exception) {
            messageLabel.setText(exception.getMessage());
        } finally {
            passwordField.clear();
        }
    }

    @FXML
    private void navigate() {
        navigate.run();
    }
}
