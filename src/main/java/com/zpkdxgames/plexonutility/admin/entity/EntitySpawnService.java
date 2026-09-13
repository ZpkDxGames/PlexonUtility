package com.zpkdxgames.plexonutility.admin.entity;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/** Safe, hard-capped entity creation at an already-loaded nearby location. */
public final class EntitySpawnService {
    private final Supplier<UtilityConfig> config;
    private final AdminAuditService audit;

    public EntitySpawnService(Supplier<UtilityConfig> config, AdminAuditService audit) {
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

    public SpawnResult spawn(CommandSender actor, Player player, EntityType type, int requested) {
        UtilityConfig.SpawnMobConfig policy = config.get().admin().entityManagement().spawnmob();
        if (!policy.enabled()) throw new IllegalStateException("disabled");
        validate(type);
        if (requested < 1 || requested > policy.maxAmount() || requested > 100) {
            throw new IllegalArgumentException("amount");
        }
        Location location = findSpawnLocation(player, type, policy.safeLocationSearch());
        if (location == null) return new SpawnResult(requested, 0, requested, null);

        int spawned = 0;
        for (int index = 0; index < requested; index++) {
            try {
                Entity created = location.getWorld().spawnEntity(location, type);
                if (created != null) spawned++;
            } catch (RuntimeException ignored) {
                // Count the failure and continue within the bounded request.
            }
        }
        int failed = requested - spawned;
        audit.log("SPAWNMOB", actor,
                "type=" + type.name() + " requested=" + requested + " spawned=" + spawned
                        + " failed=" + failed + " world=" + location.getWorld().getName());
        return new SpawnResult(requested, spawned, failed, location);
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
        Location front = player.getLocation().clone().add(player.getLocation().getDirection().normalize().multiply(2.0D));
        locations.add(front);
        for (Location base : List.copyOf(locations)) {
            locations.add(base.clone().add(1, 0, 0));
            locations.add(base.clone().add(-1, 0, 0));
            locations.add(base.clone().add(0, 0, 1));
            locations.add(base.clone().add(0, 0, -1));
            locations.add(base.clone().add(0, 1, 0));
        }
        for (Location candidate : locations) {
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

    public record SpawnResult(int requested, int spawned, int failed, Location location) { }
}
