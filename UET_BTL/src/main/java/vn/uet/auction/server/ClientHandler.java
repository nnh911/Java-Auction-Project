package vn.uet.auction.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import vn.uet.auction.protocol.Request;

final class ClientHandler implements Runnable {
    private static final int HANDSHAKE_TIMEOUT_MS = 5_000;
    private static final int IDLE_TIMEOUT_MS = 300_000;
    private static final ObjectInputFilter REQUEST_FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=12;maxarray=8192;maxrefs=100000;maxbytes=16777216;"
            + "vn.uet.auction.protocol.Request;vn.uet.auction.protocol.Request$*;"
            + "vn.uet.auction.protocol.Command;vn.uet.auction.model.item.ItemType;"
            + "vn.uet.auction.model.user.Role;java.lang.Enum;java.util.UUID;"
            + "java.math.BigDecimal;java.math.BigInteger;java.lang.Number;"
            + "java.time.Ser;java.time.Instant;!*");

    private final Socket socket;
    private final CommandDispatcher dispatcher;
    private final Session session = new Session();

    ClientHandler(Socket socket, CommandDispatcher dispatcher) {
        this.socket = socket;
        this.dispatcher = dispatcher;
    }

    @Override
    public void run() {
        try (socket) {
            socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);
            try (var output = new ObjectOutputStream(socket.getOutputStream())) {
                output.flush();
                var bytes = new RequestInput(socket.getInputStream());
                try (var input = new ObjectInputStream(bytes)) {
                    input.setObjectInputFilter(REQUEST_FILTER);
                    socket.setSoTimeout(IDLE_TIMEOUT_MS);
                    while (bytes.readRequest(input) instanceof Request request) {
                        write(output, dispatcher.dispatch(session, request));
                    }
                }
            }
        } catch (IOException | ClassNotFoundException ignored) {
            // EOF, timeout and malformed streams terminate only this session.
        } finally {
            session.clear();
        }
    }

    private static void write(ObjectOutputStream output, Object message) throws IOException {
        synchronized (output) {
            output.reset();
            output.writeObject(message);
            output.flush();
        }
    }

    // ObjectInputFilter does not check the contents of serialized Strings.
    private static final class RequestInput extends InputStream {
        private static final int MAX_REQUEST_BYTES = 65_536;
        private final InputStream source;
        private int remaining = MAX_REQUEST_BYTES;

        RequestInput(InputStream input) {
            source = input;
        }

        Object readRequest(ObjectInputStream input) throws IOException, ClassNotFoundException {
            remaining = MAX_REQUEST_BYTES;
            return input.readObject();
        }

        @Override
        public int read() throws IOException {
            checkBudget();
            int value = source.read();
            if (value != -1) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            if (length == 0) {
                return 0;
            }
            checkBudget();
            int count = source.read(buffer, offset, Math.min(length, remaining));
            if (count > 0) {
                remaining -= count;
            }
            return count;
        }

        private void checkBudget() throws IOException {
            if (remaining == 0) {
                throw new IOException("Request exceeds byte limit");
            }
        }
    }
}
