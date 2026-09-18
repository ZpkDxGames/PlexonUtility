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
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

/**
 * Runtime admin state kept separate from human-authored config.yml.
 *
 * <p>Writes are ordered, revisioned and owner-scoped through Core 2.1. Runtime callers receive an
 * observable persistence result; a failed write never masquerades as durable success. A newer
 * successful revision recovers dirty/degraded state because it contains the complete snapshot.</p>
 */
public final class AdminDataStore implements AutoCloseable {
    public static final int SCHEMA_VERSION = 2;
    private static final int MAX_WRITE_ATTEMPTS = 3;
    private static final long CLOSE_TIMEOUT_SECONDS = 3L;

    private final JavaPlugin plugin;
    private final CoreScheduler scheduler;
    private final File file;
    private final Consumer<PersistenceStatus> healthListener;
    private volatile Snapshot snapshot = Snapshot.empty();

    private CompletableFuture<Void> pendingWrite = CompletableFuture.completedFuture(null);
    private long currentRevision;
    private long persistedRevision;
    private int pendingWrites;
    private int retryCount;
    private Instant lastSuccessfulWrite;
    private String lastFailure;
    private HealthState health = HealthState.READY;
    private boolean closed;

    public AdminDataStore(JavaPlugin plugin, CoreScheduler scheduler) {
        this(plugin, scheduler, ignored -> { });
    }

    public AdminDataStore(JavaPlugin plugin, CoreScheduler scheduler,
                          Consumer<PersistenceStatus> healthListener) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.healthListener = Objects.requireNonNull(healthListener, "healthListener");
        this.file = new File(plugin.getDataFolder(), "admin-data.yml");
    }

    public synchronized void load() {
        if (!file.exists()) {
            snapshot = Snapshot.empty();
            currentRevision = 0L;
            persistedRevision = 0L;
            health = HealthState.READY;
            notifyHealth();
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(file);
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalArgumentException("admin-data.yml could not be loaded: " + exception.getMessage(), exception);
        }

        Object rawSchema = yaml.get("schema-version");
        if (!(rawSchema instanceof Number number)) {
            throw new IllegalArgumentException("admin-data.yml schema-version must be an integer");
        }
        int schema = number.intValue();
        if (schema < 1 || schema > SCHEMA_VERSION) {
            throw new IllegalArgumentException("admin-data.yml schema-version " + schema
                    + " is not supported; current schema is " + SCHEMA_VERSION);
        }

        PrisonLocation prison = readPrison(yaml, schema);
        Set<UUID> vanished = readVanished(yaml);
        snapshot = new Snapshot(prison, vanished);

        currentRevision = 0L;
        persistedRevision = 0L;
        health = HealthState.READY;

        if (schema == 1) {
            backupSchemaOne();
            currentRevision = 1L;
            health = HealthState.DIRTY;
            scheduleWrite(currentRevision, snapshot);
            plugin.getLogger().info("Scheduled admin-data.yml schema migration 1 -> " + SCHEMA_VERSION + ".");
        } else {
            notifyHealth();
        }
    }

    public Snapshot snapshot() {
        return snapshot;
    }

    public synchronized PersistenceStatus status() {
        return new PersistenceStatus(
                currentRevision,
                persistedRevision,
                persistedRevision < currentRevision,
                pendingWrites,
                lastSuccessfulWrite,
                lastFailure,
                retryCount,
                health);
    }

    public synchronized CompletableFuture<PersistenceResult> setPrison(PrisonLocation prison) {
        PrisonLocation nextPrison = Objects.requireNonNull(prison, "prison");
        if (nextPrison.equals(snapshot.prison())) return unchangedResult();
        Snapshot next = new Snapshot(nextPrison, snapshot.vanished());
        snapshot = next;
        long revision = ++currentRevision;
        health = HealthState.DIRTY;
        return scheduleWrite(revision, next);
    }

    public synchronized CompletableFuture<PersistenceResult> clearPrison() {
        if (snapshot.prison() == null) return unchangedResult();
        Snapshot next = new Snapshot(null, snapshot.vanished());
        snapshot = next;
        long revision = ++currentRevision;
        health = HealthState.DIRTY;
        return scheduleWrite(revision, next);
    }

    public synchronized CompletableFuture<PersistenceResult> setVanished(UUID playerId, boolean value) {
        Objects.requireNonNull(playerId, "playerId");
        LinkedHashSet<UUID> vanished = new LinkedHashSet<>(snapshot.vanished());
        boolean changed = value ? vanished.add(playerId) : vanished.remove(playerId);
        if (!changed) return unchangedResult();
        Snapshot next = new Snapshot(snapshot.prison(), vanished);
        snapshot = next;
        long revision = ++currentRevision;
        health = HealthState.DIRTY;
        return scheduleWrite(revision, next);
    }

    private synchronized CompletableFuture<PersistenceResult> unchangedResult() {
        return CompletableFuture.completedFuture(new PersistenceResult(
                currentRevision,
                persistedRevision >= currentRevision,
                "unchanged"));
    }

    private synchronized CompletableFuture<PersistenceResult> scheduleWrite(long revision, Snapshot target) {
        if (closed) {
            return CompletableFuture.completedFuture(new PersistenceResult(revision, false, "store-closed"));
        }

        Snapshot immutable = new Snapshot(target.prison(), target.vanished());
        pendingWrites++;
        CompletableFuture<Void> write = pendingWrite
                .handle((ignored, previousFailure) -> null)
                .thenCompose(ignored -> scheduler.runIo(plugin, () -> writeWithRetry(immutable)));
        pendingWrite = write;

        CompletableFuture<PersistenceResult> observed = new CompletableFuture<>();
        write.whenComplete((ignored, failure) -> {
            synchronized (AdminDataStore.this) {
                pendingWrites = Math.max(0, pendingWrites - 1);
                if (failure == null) {
                    persistedRevision = Math.max(persistedRevision, revision);
                    lastSuccessfulWrite = Instant.now();
                    lastFailure = null;
                    health = persistedRevision >= currentRevision ? HealthState.READY : HealthState.DIRTY;
                    observed.complete(new PersistenceResult(revision, true, "persisted"));
                } else {
                    lastFailure = rootMessage(failure);
                    health = HealthState.DEGRADED;
                    plugin.getLogger().severe("admin-data.yml revision " + revision
                            + " write failed after " + MAX_WRITE_ATTEMPTS + " attempt(s): " + lastFailure);
                    observed.complete(new PersistenceResult(revision, false, lastFailure));
                }
            }
            notifyHealth();
        });
        return observed;
    }

    private void writeWithRetry(Snapshot target) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= MAX_WRITE_ATTEMPTS; attempt++) {
            try {
                writeSnapshot(target);
                return;
            } catch (RuntimeException failure) {
                last = failure;
                if (attempt == MAX_WRITE_ATTEMPTS) break;
                synchronized (this) {
                    retryCount++;
                }
                long backoffMillis = attempt == 1 ? 50L : 150L;
                LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(backoffMillis));
                if (Thread.currentThread().isInterrupted()) {
                    Thread.currentThread().interrupt();
                    throw failure;
                }
            }
        }
        throw Objects.requireNonNull(last, "last");
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
                yaml.set("prison.world-uuid", prison.worldId() == null ? null : prison.worldId().toString());
                yaml.set("prison.world-name", prison.worldName());
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

    private PrisonLocation readPrison(YamlConfiguration yaml, int schema) {
        if (!yaml.getBoolean("prison.configured", false)) return null;

        String worldName = schema == 1 ? yaml.getString("prison.world") : yaml.getString("prison.world-name");
        if (worldName == null || worldName.isBlank()) {
            throw new IllegalArgumentException("admin-data.yml prison world name is required");
        }

        UUID worldId = null;
        if (schema >= 2) {
            String rawUuid = yaml.getString("prison.world-uuid");
            if (rawUuid != null && !rawUuid.isBlank()) {
                try {
                    worldId = UUID.fromString(rawUuid);
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException("admin-data.yml prison.world-uuid is invalid", exception);
                }
            }
        } else {
            org.bukkit.World world = plugin.getServer().getWorld(worldName);
            if (world != null) worldId = world.getUID();
        }

        return new PrisonLocation(
                worldId,
                worldName,
                finite(yaml, "prison.x"),
                finite(yaml, "prison.y"),
                finite(yaml, "prison.z"),
                (float) finite(yaml, "prison.yaw"),
                (float) finite(yaml, "prison.pitch"));
    }

    private static Set<UUID> readVanished(YamlConfiguration yaml) {
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
        return vanished;
    }

    private void backupSchemaOne() {
        try {
            Files.copy(file.toPath(),
                    file.toPath().resolveSibling(file.getName() + ".schema1.bak"),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not back up schema-1 admin-data.yml before migration", exception);
        }
    }

    private static double finite(YamlConfiguration yaml, String path) {
        Object raw = yaml.get(path);
        if (!(raw instanceof Number number)) {
            throw new IllegalArgumentException("admin-data.yml " + path + " must be numeric");
        }
        double value = number.doubleValue();
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("admin-data.yml " + path + " must be finite");
        }
        return value;
    }

    private void notifyHealth() {
        PersistenceStatus current = status();
        try {
            healthListener.accept(current);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning("AdminDataStore health listener failed: " + rootMessage(exception));
        }
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
            closed = true;
            pending = pendingWrite;
        }
        try {
            pending.get(CLOSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException exception) {
            PersistenceStatus status = status();
            plugin.getLogger().severe("Timed out waiting for admin-data.yml persistence; dirty="
                    + status.dirty() + " pending=" + status.pendingWrites()
                    + " currentRevision=" + status.currentRevision()
                    + " persistedRevision=" + status.persistedRevision());
        } catch (Exception exception) {
            plugin.getLogger().severe("Final admin-data.yml write did not complete: " + rootMessage(exception));
        } finally {
            synchronized (this) {
                health = HealthState.CLOSED;
            }
        }
    }

    public enum HealthState {
        READY,
        DIRTY,
        DEGRADED,
        CLOSED
    }

    public record PersistenceStatus(
            long currentRevision,
            long persistedRevision,
            boolean dirty,
            int pendingWrites,
            Instant lastSuccessfulWrite,
            String lastFailure,
            int retryCount,
            HealthState health) { }

    public record PersistenceResult(long revision, boolean durable, String detail) {
        public PersistenceResult {
            detail = detail == null ? "" : detail;
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
