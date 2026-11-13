package vn.uet.auction.persistence;

import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/** Lưu một snapshot nội bộ của server. Bên gọi giữ lock chung khi save. */
public final class FileStateStore implements StateStore {
    private static final ObjectInputFilter SNAPSHOT_FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=32;maxrefs=250000;maxarray=250000;maxbytes=33554432;"
            + "vn.uet.auction.persistence.ApplicationState;vn.uet.auction.persistence.AccountState;"
            + "vn.uet.auction.model.**;java.lang.String;java.lang.Enum;java.lang.Number;"
            + "java.lang.Object;java.util.HashMap;java.util.Map$Entry;java.util.ArrayList;"
            + "java.util.UUID;java.time.Ser;java.time.Instant;java.math.BigDecimal;"
            + "java.math.BigInteger;!*");

    private final Path file;

    public FileStateStore(Path file) {
        this.file = Objects.requireNonNull(file, "file").toAbsolutePath().normalize();
    }

    @Override
    public ApplicationState load() throws IOException {
        // Chỉ file không tồn tại mới được coi là lần chạy đầu.
        java.io.InputStream input;
        try {
            input = Files.newInputStream(file);
        } catch (NoSuchFileException missingFile) {
            return new ApplicationState();
        }
        try (input; ObjectInputStream stream = new ObjectInputStream(input)) {
            stream.setObjectInputFilter(SNAPSHOT_FILTER);
            if (stream.readObject() instanceof ApplicationState state) {
                return state;
            }
            throw new IOException("File không chứa snapshot hợp lệ");
        } catch (IOException | ClassNotFoundException failure) {
            throw new IOException("Không thể đọc snapshot: " + file, failure);
        }
    }

    @Override
    public void save(ApplicationState state) throws IOException {
        if (state == null) {
            throw new IllegalArgumentException("State must not be null");
        }
        Path directory = file.getParent();
        Files.createDirectories(directory);
        Path temporary = Files.createTempFile(directory, "auction-", ".tmp");
        try {
            try (ObjectOutputStream stream = new ObjectOutputStream(Files.newOutputStream(temporary))) {
                stream.writeObject(state);
            }
            // Nếu hệ thống không hỗ trợ thay file nguyên tử, báo lỗi cho transaction.
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | RuntimeException failure) {
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }
}
