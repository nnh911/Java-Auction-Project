package vn.uet.auction.client;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import vn.uet.auction.client.controller.LoginController;
import vn.uet.auction.client.controller.RegisterController;
import vn.uet.auction.client.controller.SellerController;
import vn.uet.auction.client.network.AuctionClient;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.protocol.Command;
import vn.uet.auction.protocol.Request;
import vn.uet.auction.protocol.dto.UserSummary;

public class ClientApp extends Application {
    private Stage stage;
    private int port;
    private volatile AuctionClient client;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        port = Integer.parseInt(getParameters().getNamed().getOrDefault("port", "5555"));
        stage.setTitle("UET Auction");
        showLogin();
        stage.show();
    }

    private void showLogin() {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/view/login.fxml"));
        Parent root = load(loader);
        LoginController controller = loader.getController();
        controller.configure(this, this::showRegister);
        stage.setScene(new Scene(root, 440, 360));
    }

    private void showRegister() {
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/view/register.fxml"));
        Parent root = load(loader);
        RegisterController controller = loader.getController();
        controller.configure(this, this::showLogin);
        stage.setScene(new Scene(root, 440, 460));
    }

    public boolean openSeller(UserSummary user) {
        if (user.role() != Role.SELLER) {
            return false;
        }
        FXMLLoader loader = new FXMLLoader(ClientApp.class.getResource("/view/seller.fxml"));
        Parent root = load(loader);
        SellerController controller = loader.getController();
        controller.setGateway(new SellerSocketGateway(client, user.id()));
        stage.setScene(new Scene(root));
        return true;
    }

    public CompletionStage<UserSummary> login(String username, String password) {
        return authenticate(Command.LOGIN, new Request.LoginPayload(username, password));
    }

    public CompletionStage<UserSummary> register(String username, String password, Role role) {
        return authenticate(Command.REGISTER, new Request.RegisterPayload(username, password, role));
    }

    private CompletionStage<UserSummary> authenticate(Command command, Request.Payload payload) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (client == null) {
                    client = new AuctionClient(port);
                }
                return client.call(command, payload, UserSummary.class);
            } catch (IOException e) {
                client = null;
                throw new UncheckedIOException("Không thể kết nối máy chủ đấu giá.", e);
            }
        });
    }

    @Override
    public void stop() throws IOException {
        AuctionClient connection = client;
        if (connection != null) {
            connection.close();
        }
    }

    private Parent load(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể mở màn hình " + loader.getLocation(), exception);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
