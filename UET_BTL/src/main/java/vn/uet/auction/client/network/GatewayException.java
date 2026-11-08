package vn.uet.auction.client.network;
import java.util.Objects;
import vn.uet.auction.protocol.ErrorCode;
/** Safe server failure exposed to the client without parsing message text. */
public final class GatewayException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final ErrorCode errorCode;
    public GatewayException(ErrorCode errorCode, String message) {
        super(requireMessage(message));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }
    public ErrorCode getErrorCode() { return errorCode; }
    private static String requireMessage(String message) {
        Objects.requireNonNull(message, "message");
        if (message.isBlank()) throw new IllegalArgumentException("message must not be blank");
        return message;
    }
}
