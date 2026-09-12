package com.zpkdxgames.plexonutility.admin.prison;

import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.admin.AdminDataStore;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.Optional;

/** Holding-location utility only; this service intentionally owns no sentence/jail state. */
public final class PrisonService {
    private final JavaPlugin plugin;
    private final AdminDataStore data;
    private final AdminAuditService audit;

    public PrisonService(JavaPlugin plugin, AdminDataStore data, AdminAuditService audit) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.data = Objects.requireNonNull(data, "data");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    public Optional<PrisonLocation> location() {
        return Optional.ofNullable(data.snapshot().prison());
    }

    public PrisonLocation set(Player actor) {
        PrisonLocation location = PrisonLocation.from(actor.getLocation());
        data.setPrison(location);
        audit.log("PRISON_SET", actor, "world=" + location.world() + " coordinates=" + location.coordinates());
        return location;
    }

    public boolean clear(CommandSender actor) {
        PrisonLocation previous = data.snapshot().prison();
        if (previous == null) return false;
        data.clearPrison();
        audit.log("PRISON_CLEAR", actor, "world=" + previous.world() + " coordinates=" + previous.coordinates());
        return true;
    }

    public Result gotoPrison(Player actor) {
        Optional<Location> destination = resolve();
        if (data.snapshot().prison() == null) return Result.NOT_CONFIGURED;
        if (destination.isEmpty()) return Result.WORLD_MISSING;
        return actor.teleport(destination.get()) ? Result.SUCCESS : Result.TELEPORT_FAILED;
    }

    public Result send(CommandSender actor, Player target) {
        if (!target.isOnline()) return Result.TARGET_OFFLINE;
        Optional<Location> destination = resolve();
        if (data.snapshot().prison() == null) return Result.NOT_CONFIGURED;
        if (destination.isEmpty()) return Result.WORLD_MISSING;
        if (!target.teleport(destination.get())) return Result.TELEPORT_FAILED;
        PrisonLocation location = data.snapshot().prison();
        audit.log("PRISON_SEND", actor, target,
                "world=" + location.world() + " coordinates=" + location.coordinates());
        return Result.SUCCESS;
    }

    private Optional<Location> resolve() {
        PrisonLocation location = data.snapshot().prison();
        return location == null ? Optional.empty() : location.resolve(plugin.getServer());
    }

    public enum Result {
        SUCCESS,
        NOT_CONFIGURED,
        WORLD_MISSING,
        TARGET_OFFLINE,
        TELEPORT_FAILED
    }
}
