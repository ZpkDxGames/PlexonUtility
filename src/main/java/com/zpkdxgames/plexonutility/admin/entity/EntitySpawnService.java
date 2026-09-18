package com.zpkdxgames.plexonutility.admin.entity;

import com.zpkdxgames.plexoncore.scheduler.CoreScheduler;
import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/** Safe, hard-capped entity creation at an already-loaded nearby location. */
public final class EntitySpawnService {
    static final int MAX_PER_TICK = 20;
    private static final Duration NEXT_BATCH_DELAY = Duration.ofMillis(50);

    private final JavaPlugin plugin;
    private final CoreScheduler scheduler;
    private final Supplier<UtilityConfig> config;
    private final AdminAuditService audit;

    public EntitySpawnService(JavaPlugin plugin, CoreScheduler scheduler,
                              Supplier<UtilityConfig> config, AdminAuditService audit) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.config = Objects.requireNonNull(config, "config");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    /** Compatibility constructor for unit tests and source integrations. */
    public EntitySpawnService(Supplier<UtilityConfig> config, AdminAuditService audit) {
        this.plugin = null;
        this.scheduler = null;
        this.config = Objects.requireNonNull(config, "config");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    public EntityType parseType(String raw) {
        if (raw == null || raw.isBlank()) throw new IllegalArgumentException("type");
        try {
            EntityType type = EntityType.valueOf(EntitySelector.normalize(raw));
            validate(type);
            return type;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("type", exception);
        }
    }

    /**
     * Compatibility synchronous path. Production command/GUI callers use {@link #spawnBatched}.
     * The same hard ceiling and loaded-chunk-only location search apply.
     */
    public SpawnResult spawn(CommandSender actor, Player player, EntityType type, int requested) {
        PreparedSpawn prepared = prepare(player, type, requested);
        if (prepared.location() == null) return new SpawnResult(requested, 0, requested, null);
        MutableSpawn state = new MutableSpawn(actor, type, requested, prepared.location());
        spawnChunk(state, requested);
        return finish(state);
    }

    /** Uses one owner-scoped coordinator and creates at most {@value MAX_PER_TICK} entities per tick. */
    public CompletableFuture<SpawnResult> spawnBatched(CommandSender actor, Player player,
                                                       EntityType type, int requested) {
        PreparedSpawn prepared = prepare(player, type, requested);
        if (prepared.location() == null) {
            return CompletableFuture.completedFuture(new SpawnResult(requested, 0, requested, null));
        }
        if (plugin == null || scheduler == null || requested <= MAX_PER_TICK) {
            return CompletableFuture.completedFuture(spawn(actor, player, type, requested));
        }

        MutableSpawn state = new MutableSpawn(actor, type, requested, prepared.location());
        CompletableFuture<SpawnResult> result = new CompletableFuture<>();
        runBatch(state, result);
        return result;
    }

    private PreparedSpawn prepare(Player player, EntityType type, int requested) {
        UtilityConfig.SpawnMobConfig policy = config.get().admin().entityManagement().spawnmob();
        if (!policy.enabled()) throw new IllegalStateException("disabled");
        validate(type);
        if (requested < 1 || requested > policy.maxAmount() || requested > 100) {
            throw new IllegalArgumentException("amount");
        }
        return new PreparedSpawn(findSpawnLocation(player, type, policy.safeLocationSearch()));
    }

    private void runBatch(MutableSpawn state, CompletableFuture<SpawnResult> result) {
        if (result.isDone()) return;
        if (plugin == null || !plugin.isEnabled()) {
            result.completeExceptionally(new IllegalStateException("PlexonUtility disabled during spawn batch"));
            return;
        }

        spawnChunk(state, Math.min(MAX_PER_TICK, state.remaining()));
        if (state.remaining() == 0) {
            result.complete(finish(state));
            return;
        }

        CoreScheduler.ObservedTaskHandle next = scheduler.schedulePrimaryObserved(
                plugin, NEXT_BATCH_DELAY, () -> runBatch(state, result));
        next.completion().whenComplete((ignored, error) -> {
            if (error != null && !result.isDone()) result.completeExceptionally(error);
        });
    }

    private void spawnChunk(MutableSpawn state, int count) {
        for (int index = 0; index < count; index++) {
            try {
                Entity created = state.location().getWorld().spawnEntity(state.location(), state.type());
                if (created != null) state.spawned++;
            } catch (RuntimeException ignored) {
                // Aggregate failure only; never create one task per failed entity.
            } finally {
                state.processed++;
            }
        }
    }

    private SpawnResult finish(MutableSpawn state) {
        int failed = state.requested() - state.spawned;
        audit.log("SPAWNMOB", state.actor(),
                "type=" + state.type().name() + " requested=" + state.requested()
                        + " spawned=" + state.spawned + " failed=" + failed
                        + " world=" + state.location().getWorld().getName());
        return new SpawnResult(state.requested(), state.spawned, failed, state.location());
    }

    public List<String> allowedTypeNames() {
        UtilityConfig.SpawnMobConfig policy = config.get().admin().entityManagement().spawnmob();
        List<String> names = new ArrayList<>();
        for (EntityType type : EntityType.values()) {
            if (type == EntityType.PLAYER || !type.isAlive() || !type.isSpawnable()) continue;
            if (policy.blockedEntityTypes().contains(type.name())) continue;
            names.add(type.name().toLowerCase(Locale.ROOT));
        }
        names.sort(String::compareTo);
        return List.copyOf(names);
    }

    private void validate(EntityType type) {
        UtilityConfig.SpawnMobConfig policy = config.get().admin().entityManagement().spawnmob();
        if (type == null || type == EntityType.PLAYER || !type.isAlive() || !type.isSpawnable()
                || policy.blockedEntityTypes().contains(type.name())) {
            throw new IllegalArgumentException("type");
        }
    }

    private static Location findSpawnLocation(Player player, EntityType type, boolean safeSearch) {
        World world = player.getWorld();
        List<Location> locations = new ArrayList<>();
        Block targeted = player.getTargetBlockExact(32);
        if (targeted != null) locations.add(targeted.getLocation().add(0.5D, 1.0D, 0.5D));
        Location playerLocation = player.getLocation();
        Location front = playerLocation.clone().add(playerLocation.getDirection().normalize().multiply(2.0D));
        locations.add(front);
        for (Location base : List.copyOf(locations)) {
            locations.add(base.clone().add(1, 0, 0));
            locations.add(base.clone().add(-1, 0, 0));
            locations.add(base.clone().add(0, 0, 1));
            locations.add(base.clone().add(0, 0, -1));
            locations.add(base.clone().add(0, 1, 0));
        }
        for (Location candidate : locations) {
            // This deliberately checks load state only; it never requests or loads a chunk.
            if (!world.isChunkLoaded(candidate.getBlockX() >> 4, candidate.getBlockZ() >> 4)) continue;
            if (!safeSearch || safeFor(candidate, type)) return candidate;
        }
        return null;
    }

    private static boolean safeFor(Location location, EntityType type) {
        Block feet = location.getBlock();
        Block head = feet.getRelative(BlockFace.UP);
        if (aquatic(type)) return feet.isLiquid() && head.isLiquid();
        if (!feet.isPassable() || !head.isPassable()) return false;
        Block below = feet.getRelative(BlockFace.DOWN);
        return below.getType().isSolid() || feet.isLiquid();
    }

    private static boolean aquatic(EntityType type) {
        String name = type.name();
        return name.contains("FISH") || name.contains("SQUID") || name.equals("DOLPHIN")
                || name.equals("AXOLOTL") || name.equals("TADPOLE") || name.equals("GUARDIAN")
                || name.equals("ELDER_GUARDIAN");
    }

    private record PreparedSpawn(Location location) { }

    private static final class MutableSpawn {
        private final CommandSender actor;
        private final EntityType type;
        private final int requested;
        private final Location location;
        private int processed;
        private int spawned;

        private MutableSpawn(CommandSender actor, EntityType type, int requested, Location location) {
            this.actor = actor;
            this.type = type;
            this.requested = requested;
            this.location = location;
        }

        CommandSender actor() { return actor; }
        EntityType type() { return type; }
        int requested() { return requested; }
        Location location() { return location; }
        int remaining() { return requested - processed; }
    }

    public record SpawnResult(int requested, int spawned, int failed, Location location) { }
}
