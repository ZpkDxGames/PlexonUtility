package com.zpkdxgames.plexonutility.admin.prison;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** Immutable persisted prison/holding waypoint. World UUID is authoritative when available. */
public record PrisonLocation(UUID worldId, String worldName,
                             double x, double y, double z, float yaw, float pitch) {
    public PrisonLocation {
        worldName = Objects.requireNonNull(worldName, "worldName").trim();
        if (worldName.isEmpty()) throw new IllegalArgumentException("Prison world name must not be blank");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Prison coordinates must be finite");
        }
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("Prison yaw/pitch must be finite");
        }
    }

    /** Compatibility constructor for schema-1/source callers that only know the last world name. */
    public PrisonLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        this(null, worldName, x, y, z, yaw, pitch);
    }

    public static PrisonLocation from(Location location) {
        Objects.requireNonNull(location, "location");
        World world = Objects.requireNonNull(location.getWorld(), "location world");
        return new PrisonLocation(world.getUID(), world.getName(),
                location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public Optional<Location> resolve(Server server) {
        Objects.requireNonNull(server, "server");
        World resolved = worldId == null ? null : server.getWorld(worldId);
        if (resolved == null) resolved = server.getWorld(worldName);
        if (resolved == null) return Optional.empty();
        return Optional.of(new Location(resolved, x, y, z, yaw, pitch));
    }

    /** Compatibility accessor retained for existing audit/UI code. */
    public String world() {
        return worldName;
    }

    public String coordinates() {
        return Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z);
    }
}
