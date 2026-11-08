package vn.uet.auction.server;
import java.util.Objects;
import java.util.UUID;
import vn.uet.auction.model.user.Role;
import vn.uet.auction.protocol.dto.UserSummary;
/** One trusted in-memory context per client; not a source of current permissions. */
public final class Session {
    private UserSummary user;
    public boolean isAuthenticated() { return user != null; }
    public UUID userId() { return current().id(); }
    public Role role() { return current().role(); }
    public void bind(UserSummary user) { this.user = Objects.requireNonNull(user); }
    public void clear() { user = null; }
    private UserSummary current() {
        if (user == null) throw new IllegalStateException("Not authenticated");
        return user;
    }
}
