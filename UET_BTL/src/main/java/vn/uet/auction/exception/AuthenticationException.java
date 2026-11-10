package vn.uet.auction.exception;

public final class AuthenticationException extends SecurityException {
    private static final long serialVersionUID = 1L;

    public AuthenticationException(String message) {
        super(message);
    }
}
