package vn.uet.auction.client;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import vn.uet.auction.client.controller.LoginController;
import vn.uet.auction.client.controller.RegisterController;
import vn.uet.auction.model.user.Role;

public class ClientApp extends Application {
    private Stage stage;
    private final FakeAuthGateway gateway = new FakeAuthGateway();

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle("UET Auction");
        showLogin();
        stage.show();
    }

    private void showLogin() {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/view/login.fxml"));
        Parent root = load(loader);
        LoginController controller = loader.getController();
        controller.configure(gateway, this::showRegister);
        stage.setScene(new Scene(root, 440, 360));
    }

    private void showRegister() {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/view/register.fxml"));
        Parent root = load(loader);
        RegisterController controller = loader.getController();
        controller.configure(gateway, this::showLogin);
        stage.setScene(new Scene(root, 440, 460));
    }

    private Parent load(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể mở màn hình: " + loader.getLocation(), exception);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Gateway mẫu trả phản hồi ngay và không lưu tài khoản.
    public static final class FakeAuthGateway {
        public String login(String username, String password) {
            username = readUsername(username);
            checkPassword(password);
            if (!username.equals("bidder_demo") || !password.equals("Demo123")) {
                throw new IllegalArgumentException("Sai tài khoản hoặc mật khẩu mẫu.");
            }
            return "Đăng nhập mẫu thành công: " + username;
        }

        public String register(String username, String password, Role role) {
            username = readUsername(username);
            checkPassword(password);
            if (role != Role.BIDDER && role != Role.SELLER) {
                throw new IllegalArgumentException("Chỉ được đăng ký Bidder hoặc Seller.");
            }
            if (username.equals("bidder_demo")) {
                throw new IllegalArgumentException("Tên đăng nhập mẫu đã tồn tại.");
            }
            return "Phản hồi mẫu: " + username + " - " + role + ". Chưa lưu tài khoản.";
        }

        private String readUsername(String input) {
            String username = input.strip();
            if (!username.matches("[A-Za-z0-9_]{3,30}")) {
                throw new IllegalArgumentException("Tên gồm 3–30 ký tự: chữ, số hoặc dấu _.");
            }
            return username;
        }

        private void checkPassword(String password) {
            if (password.isBlank()) {
                throw new IllegalArgumentException("Vui lòng nhập mật khẩu.");
            }
            // Không strip vì khoảng trắng thuộc mật khẩu.
        }
    }
}
