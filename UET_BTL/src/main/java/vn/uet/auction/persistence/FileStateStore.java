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

/** Local snapshot store. Replace the previous snapshot only after a complete write. */
public final class FileStateStore implements StateStore {
    private final Path path;

    public FileStateStore(Path path) {
        this.path = Objects.requireNonNull(path).toAbsolutePath().normalize();
    }

    @Override
    public ApplicationState load() throws IOException {
        java.io.InputStream source;
        try {
            source = Files.newInputStream(path);
        } catch (NoSuchFileException e) {
            return new ApplicationState();
        }
        try (source; var input = new ObjectInputStream(source)) {
            input.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
                    "maxdepth=30;maxarray=1000000;maxrefs=1000000;maxbytes=67108864;"
                    + "vn.uet.auction.persistence.ApplicationState;vn.uet.auction.persistence.AccountState;"
                    + "vn.uet.auction.model.**;java.base/java.util.HashMap;java.base/java.util.Map$Entry;"
                    + "java.base/java.util.ArrayList;java.base/java.util.UUID;java.base/java.lang.Object;"
                    + "java.base/java.lang.Enum;java.base/java.lang.Number;java.base/java.time.Ser;"
                    + "java.base/java.time.Instant;java.base/java.math.BigDecimal;java.base/java.math.BigInteger;!*"));
            if (input.readObject() instanceof ApplicationState state) {
                return state;
            }
            throw new IOException("Invalid application snapshot");
        } catch (ClassNotFoundException e) {
            throw new IOException("Unknown snapshot type", e);
        }
    }

    @Override
    public void save(ApplicationState state) throws IOException {
        Objects.requireNonNull(state);
        Path directory = path.getParent();
        Files.createDirectories(directory);
        Path temp = Files.createTempFile(directory, "auction-", ".tmp");
        try {
            try (var output = new ObjectOutputStream(Files.newOutputStream(temp))) {
                output.writeObject(state);
            }
            if (Files.size(temp) > 67_108_864) {
                throw new IOException("Application snapshot exceeds 64 MiB");
            }
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
