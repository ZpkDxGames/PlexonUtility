package com.zpkdxgames.plexonutility.admin.prison;

import com.zpkdxgames.plexoncore.scheduler.CoreScheduler;
import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.admin.AdminDataStore;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Holding-location utility only; this service intentionally owns no sentence/jail state. */
public final class PrisonService {
    private final JavaPlugin plugin;
    private final AdminDataStore data;
    private final AdminAuditService audit;
    private final CoreScheduler scheduler;

    public PrisonService(JavaPlugin plugin, AdminDataStore data, AdminAuditService audit, CoreScheduler scheduler) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.data = Objects.requireNonNull(data, "data");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
    }

    /** Compatibility constructor for tests/integrations; callbacks complete on their source thread. */
    public PrisonService(JavaPlugin plugin, AdminDataStore data, AdminAuditService audit) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.data = Objects.requireNonNull(data, "data");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.scheduler = null;
    }

    public Optional<PrisonLocation> location() {
        return Optional.ofNullable(data.snapshot().prison());
    }

    /** Completes only after the new waypoint is durable. */
    public CompletableFuture<PrisonLocation> set(Player actor) {
        PrisonLocation location = PrisonLocation.from(actor.getLocation());
        CompletableFuture<PrisonLocation> result = new CompletableFuture<>();
        data.setPrison(location).whenComplete((persistence, error) -> onPrimary(() -> {
            if (error != null || persistence == null || !persistence.durable()) {
                result.completeExceptionally(new IllegalStateException(
                        "Prison location could not be persisted: " + persistenceDetail(persistence, error)));
                return;
            }
            audit.log("PRISON_SET", actor,
                    "world=" + location.world() + " coordinates=" + location.coordinates()
                            + " revision=" + persistence.revision());
            result.complete(location);
        }));
        return result;
    }

    /** Completes true only after a configured waypoint has been durably cleared. */
    public CompletableFuture<Boolean> clear(CommandSender actor) {
        PrisonLocation previous = data.snapshot().prison();
        if (previous == null) return CompletableFuture.completedFuture(false);

        CompletableFuture<Boolean> result = new CompletableFuture<>();
        data.clearPrison().whenComplete((persistence, error) -> onPrimary(() -> {
            if (error != null || persistence == null || !persistence.durable()) {
                result.completeExceptionally(new IllegalStateException(
                        "Prison location clear could not be persisted: " + persistenceDetail(persistence, error)));
                return;
            }
            audit.log("PRISON_CLEAR", actor,
                    "world=" + previous.world() + " coordinates=" + previous.coordinates()
                            + " revision=" + persistence.revision());
            result.complete(true);
        }));
        return result;
    }

    public CompletableFuture<Result> gotoPrison(Player actor) {
        PrisonLocation persisted = data.snapshot().prison();
        if (persisted == null) return CompletableFuture.completedFuture(Result.NOT_CONFIGURED);
        Optional<Location> destination = persisted.resolve(plugin.getServer());
        if (destination.isEmpty()) return CompletableFuture.completedFuture(Result.WORLD_MISSING);
        if (!actorAvailable(actor)) return CompletableFuture.completedFuture(Result.ACTOR_OFFLINE);
        return teleport(actor, actor, destination.get(), null);
    }

    public CompletableFuture<Result> send(CommandSender actor, Player target) {
        if (!targetAvailable(target)) return CompletableFuture.completedFuture(Result.TARGET_OFFLINE);
        PrisonLocation persisted = data.snapshot().prison();
        if (persisted == null) return CompletableFuture.completedFuture(Result.NOT_CONFIGURED);
        Optional<Location> destination = persisted.resolve(plugin.getServer());
        if (destination.isEmpty()) return CompletableFuture.completedFuture(Result.WORLD_MISSING);
        if (!actorAvailable(actor)) return CompletableFuture.completedFuture(Result.ACTOR_OFFLINE);
        return teleport(actor, target, destination.get(), persisted);
    }

    private CompletableFuture<Result> teleport(CommandSender actor, Player target,
                                               Location destination, PrisonLocation persisted) {
        CompletableFuture<Result> result = new CompletableFuture<>();
        target.teleportAsync(destination).whenComplete((success, error) -> onPrimary(() -> {
            if (!actorAvailable(actor)) {
                result.complete(Result.ACTOR_OFFLINE);
                return;
            }
            if (!targetAvailable(target)) {
                result.complete(Result.TARGET_OFFLINE);
                return;
            }
            if (error != null || !Boolean.TRUE.equals(success)) {
                result.complete(Result.TELEPORT_FAILED);
                return;
            }
            if (persisted != null) {
                audit.log("PRISON_SEND", actor, target,
                        "world=" + persisted.world() + " coordinates=" + persisted.coordinates());
            }
            result.complete(Result.SUCCESS);
        }));
        return result;
    }

    private void onPrimary(Runnable task) {
        if (scheduler == null) {
            task.run();
            return;
        }
        scheduler.runPrimary(plugin, task);
    }

    private static boolean actorAvailable(CommandSender actor) {
        return !(actor instanceof Player player) || targetAvailable(player);
    }

    private static boolean targetAvailable(Player player) {
        return player != null && player.isOnline() && player.isConnected();
    }

    private static String persistenceDetail(AdminDataStore.PersistenceResult persistence, Throwable error) {
        if (error != null) {
            Throwable root = error;
            while (root.getCause() != null) root = root.getCause();
            return root.getMessage() == null ? root.getClass().getSimpleName() : root.getMessage();
        }
        return persistence == null ? "missing result" : persistence.detail();
    }

    public enum Result {
        SUCCESS,
        NOT_CONFIGURED,
        WORLD_MISSING,
        TARGET_OFFLINE,
        ACTOR_OFFLINE,
        TELEPORT_FAILED
    }
}
