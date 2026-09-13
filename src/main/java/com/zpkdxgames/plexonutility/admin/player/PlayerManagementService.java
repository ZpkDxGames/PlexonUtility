package com.zpkdxgames.plexonutility.admin.player;

import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Event-driven player administration state. God mode is intentionally restart-ephemeral in 3.5. */
public final class PlayerManagementService implements Listener {
    public static final float DEFAULT_WALK_SPEED = 0.2F;
    public static final float DEFAULT_FLY_SPEED = 0.1F;

    private final Supplier<UtilityConfig> config;
    private final AdminAuditService audit;
    private final Set<UUID> godMode = ConcurrentHashMap.newKeySet();
    private final Set<UUID> managedFlight = ConcurrentHashMap.newKeySet();

    public PlayerManagementService(Supplier<UtilityConfig> config, AdminAuditService audit) {
        this.config = Objects.requireNonNull(config, "config");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    public void setGameMode(CommandSender actor, Player target, GameMode mode) {
        target.setGameMode(Objects.requireNonNull(mode, "mode"));
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) managedFlight.remove(target.getUniqueId());
        audit.log("GAMEMODE", actor, target, "mode=" + mode.name());
    }

    public boolean canManageFlight(Player target) {
        GameMode mode = target.getGameMode();
        return mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR;
    }

    public boolean isFlightManaged(UUID playerId) { return managedFlight.contains(playerId); }

    public boolean setFlight(CommandSender actor, Player target, boolean enabled) {
        if (!canManageFlight(target)) return target.getAllowFlight();
        UUID id = target.getUniqueId();
        boolean changed = enabled ? managedFlight.add(id) : managedFlight.remove(id);
        if (enabled) {
            target.setAllowFlight(true);
        } else if (changed) {
            if (target.isFlying()) target.setFlying(false);
            target.setAllowFlight(false);
            target.setFallDistance(0.0F);
        }
        if (changed) audit.log(enabled ? "FLY_ON" : "FLY_OFF", actor, target, "state=" + enabled);
        return enabled;
    }

    public boolean toggleFlight(CommandSender actor, Player target) {
        return setFlight(actor, target, !managedFlight.contains(target.getUniqueId()));
    }

    public boolean isGodMode(UUID playerId) { return godMode.contains(playerId); }

    public boolean setGodMode(CommandSender actor, Player target, boolean enabled) {
        UUID id = target.getUniqueId();
        boolean changed = enabled ? godMode.add(id) : godMode.remove(id);
        if (changed) audit.log(enabled ? "GOD_ON" : "GOD_OFF", actor, target, "state=" + enabled);
        return enabled;
    }

    public boolean toggleGodMode(CommandSender actor, Player target) {
        return setGodMode(actor, target, !isGodMode(target.getUniqueId()));
    }

    public float setSpeed(CommandSender actor, Player target, SpeedMode mode, int level) {
        if (level < 1 || level > 10) throw new IllegalArgumentException("level");
        float value = Math.max(0.0F, Math.min(1.0F, level / 10.0F));
        applySpeed(target, mode, value);
        audit.log("SPEED", actor, target, "mode=" + mode.name() + " level=" + level + " value=" + value);
        return value;
    }

    public float resetSpeed(CommandSender actor, Player target, SpeedMode mode) {
        float value = mode == SpeedMode.WALK ? DEFAULT_WALK_SPEED : DEFAULT_FLY_SPEED;
        applySpeed(target, mode, value);
        audit.log("SPEED_RESET", actor, target, "mode=" + mode.name() + " value=" + value);
        return value;
    }

    public int clearInventory(CommandSender actor, Player target) {
        PlayerInventory inventory = target.getInventory();
        int cleared = countStacks(inventory.getStorageContents()) + countStacks(inventory.getArmorContents());
        if (!empty(inventory.getItemInOffHand())) cleared++;
        inventory.clear();
        inventory.setArmorContents(new ItemStack[] {null, null, null, null});
        inventory.setItemInOffHand(new ItemStack(Material.AIR));
        target.updateInventory();
        audit.log("CLEARINVENTORY", actor, target, "stacks=" + cleared);
        return cleared;
    }

    public void reload() {
        UtilityConfig.AdminConfig admin = config.get().admin();
        if (!admin.enabled() || !admin.playerManagement().godEnabled()) godMode.clear();
        if (!admin.enabled() || !admin.playerManagement().flyEnabled()) managedFlight.clear();
    }

    public void close() {
        godMode.clear();
        managedFlight.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!isGodMode(player.getUniqueId())) return;
        if (event.getCause() == EntityDamageEvent.DamageCause.VOID) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        managedFlight.remove(id);
        godMode.remove(id);
    }

    private static void applySpeed(Player target, SpeedMode mode, float value) {
        if (!Float.isFinite(value) || value < -1.0F || value > 1.0F) throw new IllegalArgumentException("speed");
        if (mode == SpeedMode.WALK) target.setWalkSpeed(value);
        else target.setFlySpeed(value);
    }

    private static int countStacks(ItemStack[] contents) {
        int count = 0;
        for (ItemStack item : contents) if (!empty(item)) count++;
        return count;
    }

    private static boolean empty(ItemStack item) {
        return item == null || item.getType().isAir() || item.getAmount() <= 0;
    }

    public enum SpeedMode { WALK, FLY }
}
