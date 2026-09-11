package com.zpkdxgames.plexonutility.command;

import com.zpkdxgames.plexoncore.api.PlexonCoreAPI;
import com.zpkdxgames.plexonutility.PlexonUtilityPlugin;
import com.zpkdxgames.plexonutility.afk.AfkManager;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.cooldown.CooldownService;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.message.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class UtilityAdminCommand implements CommandExecutor {
    private final PlexonUtilityPlugin plugin;
    private final CooldownService cooldowns;
    private final MessageService messages;
    private final AfkManager afk;

    public UtilityAdminCommand(PlexonUtilityPlugin plugin, CooldownService cooldowns, MessageService messages, AfkManager afk) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
        this.messages = messages;
        this.afk = afk;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("diagnostics")) {
            diagnostics(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("plexonutility.reload")) {
                messages.send(sender, "no-permission");
                return true;
            }
            try {
                plugin.reloadUtilityState();
                messages.send(sender, "reloaded");
            } catch (RuntimeException exception) {
                String reason = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
                messages.send(sender, "reload-failed", Map.of("reason", reason));
            }
            return true;
        }
        return false;
    }

    private void diagnostics(CommandSender sender) {
        UtilityConfig cfg = plugin.utilityConfig();
        PlexonCoreAPI core = plugin.core();
        sender.sendMessage(Component.text("PlexonUtility " + plugin.getPluginMeta().getVersion()));
        sender.sendMessage(Component.text("Core: " + (core == null ? "UNAVAILABLE" : core.version().pluginVersion() + " / API " + core.version().apiVersion())));
        sender.sendMessage(Component.text("Module: " + (core == null ? "UNAVAILABLE" : core.modules().find("utility").map(view -> view.state().name()).orElse("MISSING"))));
        for (Feature feature : Feature.values()) {
            sender.sendMessage(Component.text("- " + feature.id() + ": " + (cfg.enabled(feature) ? "ENABLED" : "DISABLED")));
        }
        sender.sendMessage(Component.text("Cooldown players: " + cooldowns.trackedPlayers()));
        sender.sendMessage(Component.text("AFK tracked/afk: " + afk.trackedPlayers() + "/" + afk.afkPlayers()));
        sender.sendMessage(Component.text("AFK shared schedulers: " + afk.schedulerCount()));
        sender.sendMessage(Component.text("AFK auto-timeout: " + (cfg.afk().autoTimeoutEnabled() ? (cfg.afk().timeoutNanos() / 1_000_000_000L) + "s" : "DISABLED")));
        sender.sendMessage(Component.text("AFK state persistence: EPHEMERAL"));
        sender.sendMessage(Component.text("PlaceholderAPI: " + (plugin.placeholderRegistered() ? "REGISTERED" : "UNAVAILABLE")));
        sender.sendMessage(Component.text("Database: NONE"));
        sender.sendMessage(Component.text("Essentials expansion removal: BLOCKED until TAB AFK/vanish/nickname placeholders are migrated"));
    }
}
