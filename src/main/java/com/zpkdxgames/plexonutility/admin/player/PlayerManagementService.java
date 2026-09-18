package com.zpkdxgames.plexonutility.admin.player;

import com.zpkdxgames.plexoncore.scheduler.CoreScheduler;
import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Event-driven player administration state.
 *
 * <p>God mode remains restart-ephemeral. Survival/Adventure flight is explicitly owned by
 * PlexonUtility and marked in player PDC so reconnect after an unclean stop can safely reconcile
 * only flight that Utility actually granted.</p>
 */
public final class PlayerManagementService implements Listener {
    public static final float DEFAULT_WALK_SPEED = 0.2F;
    public static final float DEFAULT_FLY_SPEED = 0.1F;
    private static final Duration FLIGHT_RECONCILE_PERIOD = Duration.ofSeconds(5);

    private final JavaPlugin plugin;
    private final Supplier<UtilityConfig> config;
    private final AdminAuditService audit;
    private final CoreScheduler scheduler;
    private final NamespacedKey flightOwnerKey;
    private final Set<UUID> godMode = ConcurrentHashMap.newKeySet();
    private final Set<UUID> managedFlight = ConcurrentHashMap.newKeySet();
    private CoreScheduler.TaskHandle flightReconcileTask;

    /** Production constructor with durable ownership and Core 2.1 owner-scoped reconciliation. */
    public PlayerManagementService(JavaPlugin plugin, Supplier<UtilityConfig> config,
                                   AdminAuditService audit, CoreScheduler scheduler) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.scheduler = Objects.requireNonNull(scheduler, "scheduler");
        this.flightOwnerKey = new NamespacedKey(plugin, "managed_flight");
    }

    /** Test/source compatibility constructor. PDC/scheduled reconciliation are unavailable. */
    public PlayerManagementService(Supplier<UtilityConfig> config, AdminAuditService audit) {
        this.plugin = null;
        this.config = Objects.requireNonNull(config, "config");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.scheduler = null;
        this.flightOwnerKey = null;
    }

    public synchronized void start() {
        reconcileOwnedFlight();
        refreshFlightReconciliation();
    }

    public void setGameMode(CommandSender actor, Player target, GameMode mode) {
        target.setGameMode(Objects.requireNonNull(mode, "mode"));
        if (mode == GameMode.CREATIVE || mode == GameMode.SPECTATOR) releaseOwnership(target);
        audit.log("GAMEMODE", actor, target, "mode=" + mode.name());
    }

    public boolean canManageFlight(Player target) {
        GameMode mode = target.getGameMode();
        return mode != GameMode.CREATIVE && mode != GameMode.SPECTATOR;
    }

    public boolean isFlightManaged(UUID playerId) {
        return managedFlight.contains(playerId);
    }

    public boolean setFlight(CommandSender actor, Player target, boolean enabled) {
        if (!canManageFlight(target)) {
            releaseOwnership(target);
            return target.getAllowFlight();
        }

        if (enabled) {
            if (!ownsFlight(target) && target.getAllowFlight()) {
                // Another system already owns this permission. Never claim or later revoke it.
                return true;
            }
            boolean changed = managedFlight.add(target.getUniqueId());
            markOwned(target);
            target.setAllowFlight(true);
            if (changed) audit.log("FLY_ON", actor, target, "state=true");
            return true;
        }

        if (!ownsFlight(target)) return target.getAllowFlight();
        revokeOwnedFlight(target, actor, "explicit-disable");
        return false;
    }

    public boolean toggleFlight(CommandSender actor, Player target) {
        return setFlight(actor, target, !ownsFlight(target));
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

    public int occupiedStacks(Player target) {
        PlayerInventory inventory = target.getInventory();
        int count = countStacks(inventory.getStorageContents()) + countStacks(inventory.getArmorContents());
        if (!empty(inventory.getItemInOffHand())) count++;
        return count;
    }

    public int inventoryFingerprint(Player target) {
        PlayerInventory inventory = target.getInventory();
        int hash = java.util.Arrays.hashCode(inventory.getStorageContents());
        hash = 31 * hash + java.util.Arrays.hashCode(inventory.getArmorContents());
        hash = 31 * hash + Objects.hashCode(inventory.getItemInOffHand());
        return hash;
    }

    public int clearInventory(CommandSender actor, Player target) {
        PlayerInventory inventory = target.getInventory();
        int cleared = countStacks(inventory.getStorageContents()) + countStacks(inventory.getArmorContents());
        if (!empty(inventory.getItemInOffHand())) cleared++;
        inventory.clear();
        inventory.setArmorContents(new ItemStack[] {null, null, null, null});
        inventory.setItemInOffHand(null);
        target.updateInventory();
        audit.log("CLEARINVENTORY", actor, target, "stacks=" + cleared);
        return cleared;
    }

    public synchronized void reload() {
        UtilityConfig.AdminConfig admin = config.get().admin();
        if (!admin.enabled() || !admin.playerManagement().godEnabled()) godMode.clear();
        reconcileOwnedFlight();
        refreshFlightReconciliation();
    }

    public synchronized void close() {
        stopFlightReconciliation();
        if (plugin != null && plugin.getServer() != null) {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (ownsFlight(player)) revokeOwnedFlight(player, player, "plugin-disable");
            }
        }
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

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!hasOwnershipMarker(player)) return;

        if (!canManageFlight(player)) {
            releaseOwnership(player);
            return;
        }

        if (flightAuthorized(player)) {
            managedFlight.add(player.getUniqueId());
            player.setAllowFlight(true);
        } else {
            revokeOwnedFlight(player, player, "join-unauthorized");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (ownsFlight(player)) revokeOwnedFlight(player, player, "quit");
        godMode.remove(player.getUniqueId());
    }

    private synchronized void refreshFlightReconciliation() {
        stopFlightReconciliation();
        if (plugin == null || scheduler == null || !flightFeatureEnabled()) return;
        scheduleNextReconciliation();
    }

    private synchronized void scheduleNextReconciliation() {
        if (plugin == null || scheduler == null || !plugin.isEnabled() || !flightFeatureEnabled()) return;
        flightReconcileTask = scheduler.schedulePrimary(plugin, FLIGHT_RECONCILE_PERIOD, () -> {
            synchronized (PlayerManagementService.this) {
                flightReconcileTask = null;
                reconcileOwnedFlight();
                scheduleNextReconciliation();
            }
        });
    }

    private synchronized void stopFlightReconciliation() {
        if (flightReconcileTask == null) return;
        flightReconcileTask.cancel();
        flightReconcileTask = null;
    }

    private void reconcileOwnedFlight() {
        if (plugin == null || plugin.getServer() == null) return;
        for (UUID id : Set.copyOf(managedFlight)) {
            Player player = plugin.getServer().getPlayer(id);
            if (player == null || !player.isOnline()) {
                managedFlight.remove(id);
                continue;
            }
            if (!canManageFlight(player)) {
                releaseOwnership(player);
                continue;
            }
            if (!flightAuthorized(player)) revokeOwnedFlight(player, player, "permission-or-feature-loss");
        }
    }

    private boolean flightFeatureEnabled() {
        UtilityConfig.AdminConfig admin = config.get().admin();
        return admin.enabled() && admin.playerManagement().flyEnabled();
    }

    private boolean flightAuthorized(Player player) {
        return flightFeatureEnabled() && player.hasPermission("plexonutility.fly");
    }

    private boolean ownsFlight(Player player) {
        return managedFlight.contains(player.getUniqueId()) || hasOwnershipMarker(player);
    }

    private boolean hasOwnershipMarker(Player player) {
        return flightOwnerKey != null
                && player.getPersistentDataContainer().has(flightOwnerKey, PersistentDataType.BYTE);
    }

    private void markOwned(Player player) {
        if (flightOwnerKey != null) {
            player.getPersistentDataContainer().set(flightOwnerKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    private void releaseOwnership(Player player) {
        managedFlight.remove(player.getUniqueId());
        if (flightOwnerKey != null) player.getPersistentDataContainer().remove(flightOwnerKey);
    }

    private void revokeOwnedFlight(Player target, CommandSender actor, String reason) {
        if (!ownsFlight(target)) return;
        releaseOwnership(target);
        if (canManageFlight(target)) {
            if (target.isFlying()) target.setFlying(false);
            target.setAllowFlight(false);
            target.setFallDistance(0.0F);
        }
        audit.log("FLY_OFF", actor, target, "state=false reason=" + reason);
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
