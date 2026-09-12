package com.zpkdxgames.plexonutility.admin.prison;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;

import java.util.Objects;
import java.util.Optional;

/** Immutable persisted prison/holding waypoint. */
public record PrisonLocation(String world, double x, double y, double z, float yaw, float pitch) {
    public PrisonLocation {
        world = Objects.requireNonNull(world, "world").trim();
        if (world.isEmpty()) throw new IllegalArgumentException("Prison world must not be blank");
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Prison coordinates must be finite");
        }
        if (!Float.isFinite(yaw) || !Float.isFinite(pitch)) {
            throw new IllegalArgumentException("Prison yaw/pitch must be finite");
        }
    }

    public static PrisonLocation from(Location location) {
        Objects.requireNonNull(location, "location");
        World world = Objects.requireNonNull(location.getWorld(), "location world");
        return new PrisonLocation(world.getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    public Optional<Location> resolve(Server server) {
        World resolved = Objects.requireNonNull(server, "server").getWorld(world);
        if (resolved == null) return Optional.empty();
        return Optional.of(new Location(resolved, x, y, z, yaw, pitch));
    }

    public String coordinates() {
        return Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z);
    }
}
