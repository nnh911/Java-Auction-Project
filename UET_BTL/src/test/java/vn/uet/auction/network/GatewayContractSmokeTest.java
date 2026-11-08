package vn.uet.auction.network;

import java.io.*;
import java.util.UUID;
import vn.uet.auction.client.network.GatewayException;
import vn.uet.auction.protocol.*;

/** Executable checks for the TV2/TV3 shared boundary; no fake production service. */
public final class GatewayContractSmokeTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static void rejects(Class<? extends Throwable> type, Runnable action) {
        try { action.run(); } catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError(error);
        }
        throw new AssertionError("Expected " + type.getName());
    }
    public static void main(String[] args) throws Exception {
        var payload = new Request.AuctionIdPayload(UUID.randomUUID());
        for (Command command : new Command[]{Command.GET_AUCTION, Command.CONFIRM_PAYMENT, Command.CANCEL_AUCTION}) {
            var request = new Request(UUID.randomUUID(), command, payload);
            var bytes = new ByteArrayOutputStream();
            try (var output = new ObjectOutputStream(bytes)) { output.writeObject(request); }
            try (var input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
                check(request.equals(input.readObject()));
            }
        }
        var error = new GatewayException(ErrorCode.NOT_FOUND, "Not found");
        check(error.getErrorCode() == ErrorCode.NOT_FOUND && error.getMessage().equals("Not found"));
        rejects(NullPointerException.class, () -> new Request.AuctionIdPayload(null));
        rejects(NullPointerException.class, () -> new GatewayException(null, "message"));
        rejects(NullPointerException.class, () -> new GatewayException(ErrorCode.NOT_FOUND, null));
        rejects(IllegalArgumentException.class, () -> new GatewayException(ErrorCode.NOT_FOUND, "  "));
        check(EventMessage.EventType.valueOf("PAYMENT_CONFIRMED") != null);
        System.out.println("GatewayContractSmokeTest PASS");
    }
}
