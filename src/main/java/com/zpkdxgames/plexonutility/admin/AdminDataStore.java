package com.zpkdxgames.plexonutility.admin;

import com.zpkdxgames.plexoncore.scheduler.CoreScheduler;
import com.zpkdxgames.plexonutility.admin.prison.PrisonLocation;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Runtime admin state kept separate from human-authored config.yml. */
public final class AdminDataStore implements AutoCloseable {
    public static final int SCHEMA_VERSION = 1;

    private final JavaPlugin plugin;
    private final CoreScheduler scheduler;
    private final File file;
    private volatile Snapshot snapshot = Snapshot.empty();
    private CompletableFuture<Void> pendingWrite = CompletableFuture.completedFuture(null);

    public AdminDataStore(JavaPlugin plugin, CoreScheduler scheduler) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.file = new File(plugin.getDataFolder(), "admin-data.yml");
    }

    public void load() {
        if (!file.exists()) {
            snapshot = Snapshot.empty();
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalArgumentException("admin-data.yml could not be loaded: " + exception.getMessage(), exception);
        }

        Object rawSchema = yaml.get("schema-version");
        if (!(rawSchema instanceof Number number) || number.intValue() != SCHEMA_VERSION) {
            throw new IllegalArgumentException("admin-data.yml schema-version must be " + SCHEMA_VERSION);
        }

        PrisonLocation prison = null;
        if (yaml.getBoolean("prison.configured", false)) {
            String world = yaml.getString("prison.world");
            if (world == null || world.isBlank()) throw new IllegalArgumentException("admin-data.yml prison.world is required");
            double x = finite(yaml, "prison.x");
            double y = finite(yaml, "prison.y");
            double z = finite(yaml, "prison.z");
            float yaw = (float) finite(yaml, "prison.yaw");
            float pitch = (float) finite(yaml, "prison.pitch");
            prison = new PrisonLocation(world, x, y, z, yaw, pitch);
        }

        Set<UUID> vanished = new LinkedHashSet<>();
        Object rawVanished = yaml.get("vanished");
        if (rawVanished != null && !(rawVanished instanceof List<?>)) {
            throw new IllegalArgumentException("admin-data.yml vanished must be a list");
        }
        for (String value : yaml.getStringList("vanished")) {
            try {
                vanished.add(UUID.fromString(value));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("admin-data.yml contains an invalid vanished UUID: " + value, exception);
            }
        }
        snapshot = new Snapshot(prison, vanished);
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public synchronized void setPrison(PrisonLocation prison) {
        Snapshot next = new Snapshot(Objects.requireNonNull(prison, "prison"), snapshot.vanished());
        snapshot = next;
        scheduleWrite(next);
    }

    public synchronized void clearPrison() {
        Snapshot next = new Snapshot(null, snapshot.vanished());
        snapshot = next;
        scheduleWrite(next);
    }

    public synchronized void setVanished(UUID playerId, boolean value) {
        LinkedHashSet<UUID> vanished = new LinkedHashSet<>(snapshot.vanished());
        boolean changed = value ? vanished.add(playerId) : vanished.remove(playerId);
        if (!changed) return;
        Snapshot next = new Snapshot(snapshot.prison(), vanished);
        snapshot = next;
        scheduleWrite(next);
    }

    private synchronized void scheduleWrite(Snapshot target) {
        Snapshot immutable = new Snapshot(target.prison(), target.vanished());
        pendingWrite = pendingWrite.handle((ignored, failure) -> null)
                .thenCompose(ignored -> scheduler.runIo(() -> writeSnapshot(immutable)));
        pendingWrite.exceptionally(failure -> {
            plugin.getLogger().severe("admin-data.yml write failed: " + rootMessage(failure));
            return null;
        });
    }

    private void writeSnapshot(Snapshot target) {
        try {
            Files.createDirectories(plugin.getDataFolder().toPath());
            Path destination = file.toPath();
            Path temporary = destination.resolveSibling(file.getName() + ".tmp");
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("schema-version", SCHEMA_VERSION);
            PrisonLocation prison = target.prison();
            yaml.set("prison.configured", prison != null);
            if (prison != null) {
                yaml.set("prison.world", prison.world());
                yaml.set("prison.x", prison.x());
                yaml.set("prison.y", prison.y());
                yaml.set("prison.z", prison.z());
                yaml.set("prison.yaw", prison.yaw());
                yaml.set("prison.pitch", prison.pitch());
            }
            yaml.set("vanished", target.vanished().stream().map(UUID::toString).sorted().toList());
            yaml.save(temporary.toFile());
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not persist admin-data.yml", exception);
        }
    }

    private static double finite(YamlConfiguration yaml, String path) {
        Object raw = yaml.get(path);
        if (!(raw instanceof Number number)) throw new IllegalArgumentException("admin-data.yml " + path + " must be numeric");
        double value = number.doubleValue();
        if (!Double.isFinite(value)) throw new IllegalArgumentException("admin-data.yml " + path + " must be finite");
        return value;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    @Override
    public void close() {
        CompletableFuture<Void> pending;
        synchronized (this) {
            pending = pendingWrite;
        }
        try {
            pending.join();
        } catch (RuntimeException exception) {
            plugin.getLogger().severe("Final admin-data.yml write did not complete: " + rootMessage(exception));
        }
    }

    public record Snapshot(PrisonLocation prison, Set<UUID> vanished) {
        public Snapshot {
            vanished = Set.copyOf(vanished == null ? Set.of() : vanished);
        }

        public static Snapshot empty() {
            return new Snapshot(null, Set.of());
        }
    }
}
