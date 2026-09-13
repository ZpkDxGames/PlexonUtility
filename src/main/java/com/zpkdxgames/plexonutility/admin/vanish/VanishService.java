package com.zpkdxgames.plexonutility.admin.vanish;

import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.admin.AdminDataStore;
import com.zpkdxgames.plexonutility.api.event.VanishStateChangeEvent;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Event-driven, plugin-aware native staff visibility authority. */
public final class VanishService {
    private final Plugin plugin;
    private final Supplier<UtilityConfig> config;
    private final AdminDataStore data;
    private final AdminAuditService audit;
    private final SyntheticPresenceBridge syntheticPresence;
    private final Set<UUID> vanished = ConcurrentHashMap.newKeySet();

    public VanishService(Plugin plugin, Supplier<UtilityConfig> config, AdminDataStore data,
                         AdminAuditService audit, SyntheticPresenceBridge syntheticPresence) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.data = Objects.requireNonNull(data, "data");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.syntheticPresence = syntheticPresence;
        if (config.get().admin().vanish().persist()) vanished.addAll(data.snapshot().vanished());
    }

    /** Compatibility constructor retained for existing tests/integrations. */
    public VanishService(Plugin plugin, Supplier<UtilityConfig> config, AdminDataStore data, AdminAuditService audit) {
        this(plugin, config, data, audit, null);
    }

    public boolean isVanished(UUID playerId) {
        return vanished.contains(playerId);
    }

    public boolean toggle(Player target, CommandSender actor) {
        return setVanished(target, !isVanished(target.getUniqueId()), actor);
    }

    public boolean setVanished(Player target, boolean value, CommandSender actor) {
        Objects.requireNonNull(target, "target");
        UUID id = target.getUniqueId();
        boolean changed = value ? vanished.add(id) : vanished.remove(id);
        if (changed && config.get().admin().vanish().persist()) data.setVanished(id, value);
        applyTarget(target);
        if (!changed) return value;

        UtilityConfig.SyntheticPresenceConfig policy = config.get().admin().vanish().syntheticPresence();
        boolean syntheticRequested = policy.enabled();
        SyntheticPresenceBridge.BroadcastResult result = syntheticPresence == null || !syntheticRequested
                ? new SyntheticPresenceBridge.BroadcastResult(0, syntheticRequested ? "bridge-unavailable" : "disabled")
                : syntheticPresence.broadcast(target, value);
        audit.log(value ? "VANISH_ON" : "VANISH_OFF", actor, target,
                "state=" + value + " syntheticAudience=" + result.recipients() + " syntheticMode=" + result.mode());
        fireStateChange(target, value, actor, syntheticRequested);
        return value;
    }

    public void onJoin(Player player) {
        UtilityConfig.VanishConfig policy = config.get().admin().vanish();
        if (policy.persist() && data.snapshot().vanished().contains(player.getUniqueId())) vanished.add(player.getUniqueId());
        applyAllToViewer(player);
        if (isVanished(player.getUniqueId())) applyTarget(player);
    }

    public void reload() {
        UtilityConfig.AdminConfig admin = config.get().admin();
        if (!admin.enabled() || !admin.vanish().enabled()) {
            restoreOwnedVisibility();
            vanished.clear();
            return;
        }
        if (admin.vanish().persist()) vanished.addAll(data.snapshot().vanished());
        for (Player viewer : Bukkit.getOnlinePlayers()) applyAllToViewer(viewer);
    }

    public String syntheticPresenceMode() {
        if (syntheticPresence == null) return "bridge-unavailable";
        return syntheticPresence.mode();
    }

    public void applyAllToViewer(Player viewer) {
        for (UUID id : vanished) {
            Player target = Bukkit.getPlayer(id);
            if (target != null && target.isOnline()) apply(viewer, target, true);
        }
    }

    private void applyTarget(Player target) {
        boolean hidden = isVanished(target.getUniqueId());
        for (Player viewer : Bukkit.getOnlinePlayers()) apply(viewer, target, hidden);
    }

    private void apply(Player viewer, Player target, boolean hidden) {
        if (viewer.getUniqueId().equals(target.getUniqueId())) return;
        if (!hidden || viewer.hasPermission("plexonutility.admin.vanish.see")) {
            viewer.showPlayer(plugin, target);
            if (viewer.canSee(target)) viewer.listPlayer(target);
            return;
        }
        viewer.hidePlayer(plugin, target);
        viewer.unlistPlayer(target);
    }

    private void fireStateChange(Player target, boolean value, CommandSender actor, boolean syntheticRequested) {
        PluginManager manager;
        try {
            manager = Bukkit.getPluginManager();
        } catch (RuntimeException exception) {
            return;
        }
        if (manager != null) manager.callEvent(new VanishStateChangeEvent(target, value, actor, syntheticRequested));
    }

    public void restoreOwnedVisibility() {
        for (UUID id : vanished) {
            Player target = Bukkit.getPlayer(id);
            if (target == null) continue;
            for (Player viewer : Bukkit.getOnlinePlayers()) {
                if (viewer.getUniqueId().equals(target.getUniqueId())) continue;
                viewer.showPlayer(plugin, target);
                if (viewer.canSee(target)) viewer.listPlayer(target);
            }
        }
    }
}
