package vn.uet.auction.client.controller;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import vn.uet.auction.model.auction.AuctionStatus;
import vn.uet.auction.protocol.dto.AuctionDetail;

/** Form tuần 7 cho một phiên. Caller cấp thao tác bất đồng bộ đã gắn phiên và danh tính. */
public final class AuctionController {
    @FXML private Label statusLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label winnerLabel;
    @FXML private Label messageLabel;
    @FXML private TextField bidAmountField;
    @FXML private Button bidButton;
    @FXML private Button refreshButton;

    private Supplier<CompletionStage<AuctionDetail>> refresh;
    private Function<BigDecimal, CompletionStage<AuctionDetail>> placeBid;
    private AuctionStatus status;
    private boolean busy;
    private boolean disposed;

    /** Gọi trên FX thread sau khi tải FXML. Callback phải trả về ngay. */
    public void configure(Supplier<CompletionStage<AuctionDetail>> refresh,
                          Function<BigDecimal, CompletionStage<AuctionDetail>> placeBid) {
        if (disposed || this.refresh != null) {
            throw new IllegalStateException("Form đã được cấu hình hoặc đóng");
        }
        Objects.requireNonNull(placeBid, "placeBid");
        this.refresh = Objects.requireNonNull(refresh, "refresh");
        this.placeBid = placeBid;
        handleRefresh();
    }

    public void dispose() {
        disposed = true;
        updateControls();
    }

    @FXML
    private void handleRefresh() {
        if (!disposed && !busy && refresh != null) {
            submit(refresh);
        }
    }

    @FXML
    private void handlePlaceBid() {
        if (disposed || busy || status != AuctionStatus.RUNNING) {
            return;
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(bidAmountField.getText().strip());
        } catch (NumberFormatException invalid) {
            messageLabel.setText("Nhập giá dưới dạng số.");
            return;
        }
        submit(() -> placeBid.apply(amount));
    }

    private void submit(Supplier<CompletionStage<AuctionDetail>> request) {
        busy = true;
        messageLabel.setText("Đang xử lý…");
        updateControls();
        try {
            request.get().whenComplete((detail, failure) ->
                    Platform.runLater(() -> complete(detail, failure)));
        } catch (RuntimeException failure) {
            complete(null, failure);
        }
    }

    private void complete(AuctionDetail detail, Throwable failure) {
        if (disposed) {
            return;
        }
        busy = false;
        if (failure == null && detail != null) {
            status = detail.status();
            statusLabel.setText(status.name());
            currentPriceLabel.setText(detail.currentPrice().toPlainString());
            winnerLabel.setText(detail.winnerId() == null ? "—" : detail.winnerId().toString());
            messageLabel.setText("");
        } else {
            messageLabel.setText("Thao tác thất bại. Hãy làm mới trạng thái và kiểm tra giá.");
        }
        updateControls();
    }

    private void updateControls() {
        boolean bidDisabled = disposed || busy || status != AuctionStatus.RUNNING;
        bidAmountField.setDisable(bidDisabled);
        bidButton.setDisable(bidDisabled);
        refreshButton.setDisable(disposed || busy || refresh == null);
    }
}
