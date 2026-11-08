package vn.uet.auction.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import vn.uet.auction.model.user.Role;

/** Prototype tuần 7: JavaFX thuần Java, gateway giả, chưa nối server. */
public class ClientApp extends Application {
    private Stage stage;
    private final FakeAuthGateway gateway = new FakeAuthGateway();

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("UET Auction - TV4 tuần 7");
        showLogin();
        stage.show();
    }

    private void showLogin() {
        Label title = new Label("ĐĂNG NHẬP MẪU");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Tên đăng nhập");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mật khẩu");
        Label messageLabel = new Label("Dùng bidder_demo / Demo123 để thử.");
        messageLabel.setWrapText(true);
        Button loginButton = new Button("Đăng nhập");
        Button registerButton = new Button("Chuyển sang đăng ký");

        loginButton.setOnAction(event -> {
            try {
                String username = readUsername(usernameField.getText());
                String password = readPassword(passwordField.getText());
                messageLabel.setText(gateway.login(username, password));
            } catch (IllegalArgumentException exception) {
                messageLabel.setText(exception.getMessage());
            } finally {
                passwordField.clear();
            }
        });
        registerButton.setOnAction(event -> showRegister());
        VBox root = new VBox(12, title, usernameField, passwordField,
                loginButton, registerButton, messageLabel);
        root.setPadding(new Insets(24));
        stage.setScene(new Scene(root, 440, 360));
    }

    private void showRegister() {
        Label title = new Label("ĐĂNG KÝ MẪU");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Tên đăng nhập");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mật khẩu");
        PasswordField confirmField = new PasswordField();
        confirmField.setPromptText("Nhập lại mật khẩu");
        ComboBox<Role> roleBox = new ComboBox<>();
        roleBox.getItems().addAll(Role.BIDDER, Role.SELLER);
        roleBox.setValue(Role.BIDDER);
        Label messageLabel = new Label("Form mẫu: chưa lưu tài khoản.");
        messageLabel.setWrapText(true);
        Button registerButton = new Button("Đăng ký");
        Button backButton = new Button("Quay lại đăng nhập");

        registerButton.setOnAction(event -> {
            try {
                String username = readUsername(usernameField.getText());
                String password = readPassword(passwordField.getText());
                if (!password.equals(confirmField.getText())) {
                    throw new IllegalArgumentException("Hai mật khẩu chưa giống nhau.");
                }
                messageLabel.setText(gateway.register(username, password, roleBox.getValue()));
            } catch (IllegalArgumentException exception) {
                messageLabel.setText(exception.getMessage());
            } finally {
                passwordField.clear();
                confirmField.clear();
            }
        });
        backButton.setOnAction(event -> showLogin());
        VBox root = new VBox(12, title, usernameField, passwordField, confirmField,
                roleBox, registerButton, backButton, messageLabel);
        root.setPadding(new Insets(24));
        stage.setScene(new Scene(root, 440, 460));
    }

    private String readUsername(String input) {
        String username = input.strip();
        if (!username.matches("[A-Za-z0-9_]{3,30}")) {
            throw new IllegalArgumentException("Tên gồm 3–30 ký tự: chữ, số hoặc dấu _.");
        }
        return username;
    }

    private String readPassword(String password) {
        if (password.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập mật khẩu.");
        }
        // Không strip vì khoảng trắng thuộc mật khẩu.
        return password;
    }

    public static void main(String[] args) { launch(args); }

    // Dữ liệu demo công khai. Không lưu mật khẩu thật, không log credentials.
    // Fake chạy tức thì. Gateway mạng sau này phải bất đồng bộ, không block FX thread.
    private static class FakeAuthGateway {
        public String login(String username, String password) {
            if (!username.equals("bidder_demo") || !password.equals("Demo123")) {
                throw new IllegalArgumentException("Sai tài khoản hoặc mật khẩu mẫu.");
            }
            return "Đăng nhập mẫu thành công: " + username;
        }

        public String register(String username, String password, Role role) {
            if (role != Role.BIDDER && role != Role.SELLER) {
                throw new IllegalArgumentException("Chỉ được đăng ký Bidder hoặc Seller.");
            }
            if (username.equals("bidder_demo")) {
                throw new IllegalArgumentException("Tên đăng nhập mẫu đã tồn tại.");
            }
            return "Phản hồi mẫu: " + username + " - " + role + ". Chưa lưu tài khoản.";
        }
    }
}
