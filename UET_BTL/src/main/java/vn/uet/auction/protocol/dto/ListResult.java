package vn.uet.auction.protocol.dto;
import java.io.Serializable;
import java.util.List;
/** Immutable serializable envelope for list commands. */
public record ListResult<T extends Serializable>(List<T> values) implements Serializable {
    private static final long serialVersionUID = 1L;
    public ListResult { values = List.copyOf(values); }
}
