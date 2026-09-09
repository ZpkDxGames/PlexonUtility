package com.zpkdxgames.plexonutility.command;

import com.zpkdxgames.plexoncore.api.PlexonCoreAPI;
import com.zpkdxgames.plexonutility.PlexonUtilityPlugin;
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

    public UtilityAdminCommand(PlexonUtilityPlugin plugin, CooldownService cooldowns, MessageService messages) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("diagnostics")) {
            diagnostics(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("reload")) {
            try {
                plugin.reloadUtilityConfig();
                messages.reload();
                messages.send(sender, "reloaded");
            } catch (RuntimeException exception) {
                messages.send(sender, "reload-failed", Map.of("reason", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()));
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
        sender.sendMessage(Component.text("AFK: NOT IMPLEMENTED (production dependency not established)"));
        sender.sendMessage(Component.text("Database: NONE"));
        sender.sendMessage(Component.text("Essentials decommission: BLOCKED pending production command/Vault/dependency audit"));
        sender.sendMessage(Component.text("Standard command labels configured: " + cfg.claimStandardCommands()));
    }
}
