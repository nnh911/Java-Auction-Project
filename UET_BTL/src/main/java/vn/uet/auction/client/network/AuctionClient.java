package vn.uet.auction.client.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.UUID;
import vn.uet.auction.protocol.Command;
import vn.uet.auction.protocol.Request;
import vn.uet.auction.protocol.Response;
import vn.uet.auction.protocol.WireMessage;

/** Blocking request client. GUI callers must invoke it off the JavaFX thread. */
public final class AuctionClient implements AutoCloseable {
    private final Socket socket = new Socket();
    private final DataInputStream input;
    private final DataOutputStream output;

    public AuctionClient(int port) throws IOException {
        try {
            socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 5_000);
            socket.setSoTimeout(10_000);
            input = new DataInputStream(socket.getInputStream());
            output = new DataOutputStream(socket.getOutputStream());
        } catch (IOException | RuntimeException e) {
            try {
                close();
            } catch (IOException closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    public synchronized Response<?> send(Command command, Request.Payload payload) throws IOException {
        Request request = new Request(UUID.randomUUID(), command, payload);
        try {
            WireMessage.write(output, request);
            Object message = WireMessage.read(input, WireMessage.RESPONSES);
            if (message instanceof Response<?> response && request.requestId().equals(response.requestId())) {
                return response;
            }
            throw new IOException("Invalid response from server");
        } catch (IOException e) {
            try {
                close();
            } catch (IOException closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    public <T> T call(Command command, Request.Payload payload, Class<T> resultType) throws IOException {
        Response<?> response = send(command, payload);
        if (!response.success()) {
            throw new GatewayException(response.errorCode(), response.message());
        }
        if (!resultType.isInstance(response.data())) {
            throw new IOException("Unexpected result type");
        }
        return resultType.cast(response.data());
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
