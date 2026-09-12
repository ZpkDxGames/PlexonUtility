package com.zpkdxgames.plexonutility.afk;

import com.zpkdxgames.plexoncore.scheduler.CoreScheduler;
import com.zpkdxgames.plexonutility.api.event.AfkStateChangeEvent;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.feedback.FeedbackService;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Bukkit adapter around {@link AfkTracker}. The only repeating work is one shared O(online players)
 * scan. Activity listeners perform constant-time checks and never do filesystem/database I/O.
 */
public final class AfkManager implements Listener, AutoCloseable {
    private final JavaPlugin plugin;
    private final Supplier<UtilityConfig> config;
    private final AfkTracker tracker;
    private final AfkNotifier notifier;
    private final SharedScheduler scheduler;
    private final CoreScheduler coreScheduler;

    public AfkManager(JavaPlugin plugin, Supplier<UtilityConfig> config, FeedbackService feedback,
                      AfkTracker tracker, CoreScheduler coreScheduler) {
        this.plugin = plugin;
        this.config = config;
        this.tracker = tracker;
        this.notifier = new AfkNotifier(config, feedback);
        this.coreScheduler = coreScheduler;
        this.scheduler = new SharedScheduler((periodTicks, task) -> {
            var handle = plugin.getServer().getScheduler().runTaskTimer(plugin, task, periodTicks, periodTicks);
            return handle::cancel;
        });
    }

    public void start() {
        if (!config.get().enabled(Feature.AFK)) return;
        for (Player player : plugin.getServer().getOnlinePlayers()) tracker.join(player.getUniqueId());
        reload();
    }

    public void reload() {
        UtilityConfig cfg = config.get();
        if (!cfg.enabled(Feature.AFK)) {
            scheduler.stop();
            for (Player player : plugin.getServer().getOnlinePlayers()) notifier.clear(player);
            tracker.clear();
            return;
        }

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            tracker.ensureTracked(player.getUniqueId());
            notifier.refresh(player, tracker.isAfk(player.getUniqueId()));
        }
        if (cfg.afk().autoTimeoutEnabled()) {
            scheduler.restart(cfg.afk().scanIntervalTicks(), this::scanOnlinePlayers);
        } else {
            scheduler.stop();
        }
    }

    public boolean toggle(Player player) {
        AfkTracker.Transition transition = tracker.toggle(player.getUniqueId());
        notifyTransition(player, transition, AfkStateChangeEvent.Reason.MANUAL);
        return tracker.isAfk(player.getUniqueId());
    }

    public boolean isAfk(UUID playerId) {
        return tracker.isAfk(playerId);
    }

    public int trackedPlayers() {
        return tracker.trackedPlayers();
    }

    public int afkPlayers() {
        return tracker.afkPlayers();
    }

    public int schedulerCount() {
        return scheduler.activeTaskCount();
    }

    public boolean schedulerRunning() {
        return scheduler.running();
    }

    private void scanOnlinePlayers() {
        UtilityConfig cfg = config.get();
        if (!cfg.enabled(Feature.AFK) || !cfg.afk().autoTimeoutEnabled()) return;
        long timeoutNanos = cfg.afk().timeoutNanos();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            AfkTracker.Transition transition = tracker.evaluateTimeout(
                    player.getUniqueId(),
                    timeoutNanos,
                    player.hasPermission("plexonutility.afk.auto.bypass"));
            if (transition != AfkTracker.Transition.NONE) {
                notifyTransition(player, transition, AfkStateChangeEvent.Reason.TIMEOUT);
            }
        }
    }

    private void activity(Player player) {
        if (!config.get().enabled(Feature.AFK)) return;
        AfkTracker.Transition transition = tracker.activity(player.getUniqueId());
        if (transition != AfkTracker.Transition.NONE) {
            notifyTransition(player, transition, AfkStateChangeEvent.Reason.ACTIVITY);
        }
    }

    private void asyncActivity(Player player) {
        if (!config.get().enabled(Feature.AFK)) return;
        AfkTracker.Transition transition = tracker.activity(player.getUniqueId());
        if (transition != AfkTracker.Transition.NONE) {
            coreScheduler.runPrimary(() -> {
                if (plugin.isEnabled()) {
                    notifyTransition(player, transition, AfkStateChangeEvent.Reason.ACTIVITY);
                }
            });
        }
    }

    private void notifyTransition(Player player, AfkTracker.Transition transition, AfkStateChangeEvent.Reason reason) {
        if (transition == AfkTracker.Transition.NONE) return;
        boolean afk = transition == AfkTracker.Transition.TO_AFK;
        plugin.getServer().getPluginManager().callEvent(new AfkStateChangeEvent(player, afk, reason));
        notifier.notify(player, transition);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (config.get().enabled(Feature.AFK)) tracker.join(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        notifier.clear(event.getPlayer());
        tracker.quit(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!config.get().afk().resetOnMovement()) return;
        Location to = event.getTo();
        if (to == null || !meaningfulMovement(event.getFrom(), to)) return;
        activity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        if (config.get().afk().resetOnChat()) asyncActivity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!config.get().afk().resetOnCommand() || isAfkCommand(event.getMessage())) return;
        activity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (config.get().afk().resetOnInteraction()) activity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (config.get().afk().resetOnInteraction()) activity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (config.get().afk().resetOnInteraction() && event.getWhoClicked() instanceof Player player) activity(player);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (config.get().afk().resetOnBlockChange()) activity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (config.get().afk().resetOnBlockChange()) activity(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!config.get().afk().resetOnDamage()) return;
        if (event.getEntity() instanceof Player player) activity(player);
        if (event instanceof EntityDamageByEntityEvent byEntity && byEntity.getDamager() instanceof Player attacker) activity(attacker);
    }

    static boolean meaningfulMovement(Location from, Location to) {
        if (from.getWorld() != to.getWorld()) return true;
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ();
    }

    static boolean isAfkCommand(String message) {
        if (message == null) return false;
        String normalized = message.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("/afk")
                || normalized.startsWith("/afk ")
                || normalized.equals("/plexonutility:afk")
                || normalized.startsWith("/plexonutility:afk ");
    }

    @Override
    public void close() {
        scheduler.close();
        for (Player player : plugin.getServer().getOnlinePlayers()) notifier.clear(player);
        tracker.clear();
    }
}
