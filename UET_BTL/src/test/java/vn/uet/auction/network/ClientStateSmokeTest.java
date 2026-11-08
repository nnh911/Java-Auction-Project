package vn.uet.auction.network;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import vn.uet.auction.client.model.ClientState;
import vn.uet.auction.model.auction.AuctionStatus;
import vn.uet.auction.protocol.dto.AuctionDetail;
import vn.uet.auction.protocol.dto.AuctionSummary;

/** Test console cho state prototype; không mô phỏng API của module khác. */
public final class ClientStateSmokeTest {
    private static void check(boolean value) {
        if (!value) throw new AssertionError("Client state mismatch");
    }

    public static void main(String[] args) {
        UUID id = UUID.randomUUID(), item = UUID.randomUUID(), seller = UUID.randomUUID();
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        var summary = new AuctionSummary(id, item, seller, BigDecimal.TEN,
                AuctionStatus.OPEN, start, start.plusSeconds(60));
        var detail = new AuctionDetail(id, item, seller, BigDecimal.TEN, BigDecimal.TEN,
                AuctionStatus.OPEN, start, start.plusSeconds(60), null, List.of());
        var state = new ClientState();
        var source = new ArrayList<>(List.of(summary));
        state.replaceAuctions(source);
        source.clear();
        check(state.getAuctions().size() == 1);
        state.selectAuction(id);
        check(state.acceptSelectedAuction(detail));
        check(state.getSelectedAuction().orElseThrow().equals(detail));
        try { state.replaceAuctions(List.of(summary, summary)); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { }
        check(state.getSelectedAuction().isPresent());
        try { state.selectAuction(UUID.randomUUID()); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { }
        check(state.getSelectedAuctionId().orElseThrow().equals(id));
        state.replaceAuctions(List.of(summary));
        check(state.getSelectedAuctionId().orElseThrow().equals(id));
        check(state.getSelectedAuction().isEmpty());
        state.replaceAuctions(List.of());
        check(state.getSelectedAuctionId().isEmpty());
        check(!state.acceptSelectedAuction(detail));
        state.replaceAuctions(List.of(summary));
        state.selectAuction(id);
        state.acceptSelectedAuction(detail);
        state.clear();
        check(state.getAuctions().isEmpty() && state.getSelectedAuctionId().isEmpty()
                && state.getSelectedAuction().isEmpty());
        System.out.println("ClientStateSmokeTest PASS");
    }
}
