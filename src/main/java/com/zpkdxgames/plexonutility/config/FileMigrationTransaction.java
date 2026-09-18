package com.zpkdxgames.plexonutility.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Small multi-file migration transaction. All candidate bytes are staged before any operator file
 * is replaced. Existing files are backed up and restored on commit failure.
 */
public final class FileMigrationTransaction {
    private FileMigrationTransaction() { }

    public static void commit(List<Update> requested) {
        List<Update> updates = requested.stream().filter(update -> update.migrations() > 0).toList();
        if (updates.isEmpty()) return;

        List<Staged> staged = new ArrayList<>();
        List<Staged> committed = new ArrayList<>();
        try {
            for (Update update : updates) {
                Path target = update.target();
                Path parent = target.toAbsolutePath().getParent();
                if (parent != null) Files.createDirectories(parent);
                Path temp = target.resolveSibling(target.getFileName() + ".tmp");
                Path backup = target.resolveSibling(target.getFileName() + ".bak");
                Files.writeString(temp, update.content(), StandardCharsets.UTF_8);
                if (update.sourceExisted() && Files.exists(target)) {
                    Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                }
                staged.add(new Staged(update, temp, backup));
            }

            for (Staged item : staged) {
                move(item.temp(), item.update().target());
                committed.add(item);
            }
        } catch (IOException failure) {
            for (int i = committed.size() - 1; i >= 0; i--) {
                Staged item = committed.get(i);
                try {
                    if (item.update().sourceExisted() && Files.exists(item.backup())) {
                        Files.copy(item.backup(), item.update().target(), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        Files.deleteIfExists(item.update().target());
                    }
                } catch (IOException ignored) {
                    failure.addSuppressed(ignored);
                }
            }
            throw new IllegalArgumentException("Configuration migration transaction failed: " + failure.getMessage(), failure);
        } finally {
            for (Staged item : staged) {
                try { Files.deleteIfExists(item.temp()); } catch (IOException ignored) { }
            }
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public record Update(Path target, String content, int migrations, boolean sourceExisted) {
        public Update {
            Objects.requireNonNull(target, "target");
            Objects.requireNonNull(content, "content");
            if (migrations < 0) throw new IllegalArgumentException("migrations");
        }
    }

    private record Staged(Update update, Path temp, Path backup) { }
}
