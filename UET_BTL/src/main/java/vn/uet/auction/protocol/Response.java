package vn.uet.auction.protocol;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public record Response<T extends Serializable>(
        UUID requestId,
        boolean success,
        ErrorCode errorCode,
        String message,
        T data
) implements Serializable {

    private static final long serialVersionUID = 1L;

    public Response {
        Objects.requireNonNull(requestId, "requestId");
        Objects.requireNonNull(message, "message");

        if (success) {
            if (errorCode != null) {
                throw new IllegalArgumentException(
                        "Successful response must not have errorCode");
            }
            Objects.requireNonNull(data, "data");
        } else {
            Objects.requireNonNull(errorCode, "errorCode");

            if (message.isBlank()) {
                throw new IllegalArgumentException(
                        "Failure response must have a message");
            }
            if (data != null) {
                throw new IllegalArgumentException(
                        "Failure response must not have data");
            }
        }
    }

    public static <T extends Serializable> Response<T> ok(
            UUID requestId, T data) {
        return new Response<>(requestId, true, null, "", data);
    }

    public static <T extends Serializable> Response<T> fail(
            UUID requestId, ErrorCode errorCode, String message) {
        return new Response<>(
                requestId, false, errorCode, message, null);
    }
}
