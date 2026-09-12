package com.zpkdxgames.plexonutility.feedback;

import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.message.MessageService;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Central presentation policy for routine utility feedback.
 *
 * <p>Persistent personal state uses bossbars, short personal confirmations use actionbars,
 * social events use compact prefixless chat by default, and errors/admin output remain normal chat.
 * No scheduler is created by this service.</p>
 */
public final class FeedbackService implements AutoCloseable {
    private final JavaPlugin plugin;
    private final Supplier<UtilityConfig> config;
    private final MessageService messages;
    private final LongSupplier nanoTime;
    private final Map<UUID, BossBar> afkBars = new ConcurrentHashMap<>();
    /** Timestamp only for AFK cycles whose public entry line was actually emitted. */
    private final Map<UUID, Long> announcedAfkSince = new ConcurrentHashMap<>();

    public FeedbackService(JavaPlugin plugin, Supplier<UtilityConfig> config, MessageService messages) {
        this(plugin, config, messages, System::nanoTime);
    }

    FeedbackService(JavaPlugin plugin, Supplier<UtilityConfig> config, MessageService messages, LongSupplier nanoTime) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.nanoTime = nanoTime;
    }

    /** Routine successful self-feedback; players receive an actionbar by default. */
    public void success(CommandSender sender, String key) {
        success(sender, key, Map.of());
    }

    public void success(CommandSender sender, String key, Map<String, String> replacements) {
        if (sender instanceof Player player && config.get().feedback().utilitySuccessActionbar()) {
            player.sendActionBar(messages.renderUnprefixed(key, replacements));
            return;
        }
        messages.send(sender, key, replacements);
    }

    public void afkEntered(Player player, boolean announce) {
        UUID playerId = player.getUniqueId();
        showAfkBar(player);
        if (!announce) {
            announcedAfkSince.remove(playerId);
            return;
        }
        announcedAfkSince.put(playerId, nanoTime.getAsLong());
        broadcastExcept(player, "afk-announcement-on", Map.of("player", player.getName()));
    }

    public void afkExited(Player player, boolean announce) {
        UUID playerId = player.getUniqueId();
        hideAfkBar(player);
        if (config.get().feedback().afkReturnActionbarEnabled()) {
            player.sendActionBar(messages.renderUnprefixed("afk-return-actionbar", Map.of()));
        }

        Long enteredAt = announcedAfkSince.remove(playerId);
        if (!announce || enteredAt == null) return;
        long minimum = config.get().feedback().afkSuppressShortReturnNanos();
        if (minimum > 0L && nanoTime.getAsLong() - enteredAt < minimum) return;
        broadcastExcept(player, "afk-announcement-off", Map.of("player", player.getName()));
    }

    /** Reconciles a persistent AFK bar after reload without generating chat or actionbar output. */
    public void refreshAfkState(Player player, boolean afk) {
        if (!afk || !config.get().feedback().afkBossbarEnabled()) {
            hideAfkBar(player);
            return;
        }
        showAfkBar(player);
    }

    public void clearPlayer(Player player) {
        hideAfkBar(player);
        announcedAfkSince.remove(player.getUniqueId());
    }

    public int activeBossbars() {
        return afkBars.size();
    }

    private void showAfkBar(Player player) {
        if (!config.get().feedback().afkBossbarEnabled()) {
            hideAfkBar(player);
            return;
        }

        BossBar bar = BossBar.bossBar(
                messages.renderUnprefixed("afk-bossbar", Map.of()),
                1.0F,
                bossbarColor(config.get().feedback().afkBossbarColor()),
                bossbarOverlay(config.get().feedback().afkBossbarOverlay()));

        BossBar previous = afkBars.put(player.getUniqueId(), bar);
        if (previous != null) player.hideBossBar(previous);
        player.showBossBar(bar);
    }

    private void hideAfkBar(Player player) {
        BossBar bar = afkBars.remove(player.getUniqueId());
        if (bar != null) player.hideBossBar(bar);
    }

    private void broadcastExcept(Player subject, String key, Map<String, String> replacements) {
        Component component = config.get().feedback().socialEventPrefix()
                ? messages.render(key, replacements)
                : messages.renderUnprefixed(key, replacements);
        UUID subjectId = subject.getUniqueId();
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (!online.getUniqueId().equals(subjectId)) online.sendMessage(component);
        }
    }

    static BossBar.Color bossbarColor(String value) {
        return BossBar.Color.valueOf(value);
    }

    static BossBar.Overlay bossbarOverlay(String value) {
        return BossBar.Overlay.valueOf(value);
    }

    @Override
    public void close() {
        for (Player player : plugin.getServer().getOnlinePlayers()) hideAfkBar(player);
        afkBars.clear();
        announcedAfkSince.clear();
    }
}
