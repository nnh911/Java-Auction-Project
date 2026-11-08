package vn.uet.auction.persistence;
import java.io.Serializable;
import java.util.Objects;
/** Internal credentials; never return through the protocol. Immutable for snapshot copies. */
public record AccountState(String passwordHash, boolean active) implements Serializable {
    private static final long serialVersionUID = 1L;
    public AccountState { Objects.requireNonNull(passwordHash, "passwordHash"); }
    @Override public String toString() { return "AccountState[passwordHash=REDACTED, active=" + active + "]"; }
}
