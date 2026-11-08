package vn.uet.auction.client.network;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import vn.uet.auction.protocol.dto.AuctionDetail;

/**
 * Async client boundary, independent of Socket. Implementations must reject null
 * arguments immediately, return non-null stages and non-null successful details.
 * Server failures complete exceptionally with GatewayException; I/O and timeout
 * failures with IOException and TimeoutException. Never block the FX thread.
 * Events signal refresh, not ordered state updates. Register each listener once
 * by equals; removal of an absent listener is harmless. Isolate and log listener
 * RuntimeExceptions. Controllers marshal rendering to the FX thread and coalesce
 * refreshes per auction; callbacks may execute on background threads.
 */
public interface AuctionGateway {
    CompletionStage<AuctionDetail> getAuction(UUID auctionId);
    CompletionStage<AuctionDetail> placeBid(UUID auctionId, BigDecimal amount);
    CompletionStage<AuctionDetail> confirmPayment(UUID auctionId);
    CompletionStage<AuctionDetail> cancelAuction(UUID auctionId);
    void addEventListener(ClientEventListener listener);
    void removeEventListener(ClientEventListener listener);
}
