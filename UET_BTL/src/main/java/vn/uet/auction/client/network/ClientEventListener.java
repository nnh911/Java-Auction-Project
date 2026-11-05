package vn.uet.auction.client.network;
import vn.uet.auction.protocol.EventMessage;
/** Receives a non-null refresh signal; callback is not necessarily on the FX thread. */
@FunctionalInterface
public interface ClientEventListener {
    void onEvent(EventMessage event);
}
