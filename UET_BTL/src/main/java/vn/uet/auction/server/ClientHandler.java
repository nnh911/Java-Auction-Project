package vn.uet.auction.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import vn.uet.auction.protocol.Request;
import vn.uet.auction.protocol.WireMessage;

/** Keeps authentication state local to one TCP connection. */
final class ClientHandler implements Runnable {
    private final Socket socket;
    private final CommandDispatcher dispatcher;

    ClientHandler(Socket socket, CommandDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        Session session = new Session();
        try (socket;
             var input = new DataInputStream(socket.getInputStream());
             var output = new DataOutputStream(socket.getOutputStream())) {
            while (WireMessage.read(input, WireMessage.REQUESTS) instanceof Request request) {
                WireMessage.write(output, dispatcher.dispatch(session, request));
            }
        } catch (IOException e) {
            // A disconnected client or invalid message ends only its own session.
        } finally {
            session.clear();
        }
    }
}
