package com.zpkdxgames.plexonutility.command;

import com.zpkdxgames.plexoncore.api.PlexonCoreAPI;
import com.zpkdxgames.plexonutility.PlexonUtilityPlugin;
import com.zpkdxgames.plexonutility.afk.AfkManager;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.cooldown.CooldownService;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.integration.ComplementService;
import com.zpkdxgames.plexonutility.integration.FamilyCompatibilityService;
import com.zpkdxgames.plexonutility.message.MessageService;
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
    private final ComplementService complements;
    private final FamilyCompatibilityService family;

    public UtilityAdminCommand(PlexonUtilityPlugin plugin, CooldownService cooldowns, MessageService messages,
                               AfkManager afk, ComplementService complements, FamilyCompatibilityService family) {
        this.plugin = plugin;
        this.cooldowns = cooldowns;
        this.messages = messages;
        this.afk = afk;
        this.complements = complements;
        this.family = family;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("diagnostics")) {
            diagnostics(sender);
            return true;
        }
        if (args.length > 1) return false;
        if (args[0].equalsIgnoreCase("integrations")) {
            integrations(sender);
            return true;
        }
        if (args[0].equalsIgnoreCase("family")) {
            family(sender);
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
        messages.sendRaw(sender, "<gradient:#57E389:#22D3EE><bold>PlexonUtility Diagnostics</bold></gradient>");
        messages.sendRaw(sender, "<gray>Plugin:</gray> <white><version></white>", Map.of("version", plugin.getPluginMeta().getVersion()));
        messages.sendRaw(sender, "<gray>Core:</gray> <white><core></white>", Map.of("core", core == null ? "UNAVAILABLE" : core.version().pluginVersion() + " / API " + core.version().apiVersion()));
        messages.sendRaw(sender, "<gray>Module:</gray> <white><state></white>", Map.of("state", core == null ? "UNAVAILABLE" : core.modules().find("utility").map(view -> view.state().name()).orElse("MISSING")));
        for (Feature feature : Feature.values()) {
            messages.sendRaw(sender, "<dark_gray>•</dark_gray> <gray><feature>:</gray> <state>", Map.of(
                    "feature", feature.id(),
                    "state", cfg.enabled(feature) ? "ENABLED" : "DISABLED"));
        }
        messages.sendRaw(sender, "<gray>Cooldown players:</gray> <white><count></white>", Map.of("count", cooldowns.trackedPlayers()));
        messages.sendRaw(sender, "<gray>AFK tracked/afk:</gray> <white><tracked>/<afk></white>", Map.of("tracked", afk.trackedPlayers(), "afk", afk.afkPlayers()));
        messages.sendRaw(sender, "<gray>AFK shared schedulers:</gray> <white><count></white>", Map.of("count", afk.schedulerCount()));
        messages.sendRaw(sender, "<gray>AFK auto-timeout:</gray> <white><value></white>", Map.of("value", cfg.afk().autoTimeoutEnabled() ? (cfg.afk().timeoutNanos() / 1_000_000_000L) + "s" : "DISABLED"));
        messages.sendRaw(sender, "<gray>AFK bossbar:</gray> <white><state></white> <dark_gray>•</dark_gray> <white><color>/<overlay></white>", Map.of(
                "state", cfg.feedback().afkBossbarEnabled() ? "ENABLED" : "DISABLED",
                "color", cfg.feedback().afkBossbarColor(),
                "overlay", cfg.feedback().afkBossbarOverlay()));
        messages.sendRaw(sender, "<gray>Quiet self success:</gray> <white><state></white>", Map.of("state", cfg.feedback().utilitySuccessActionbar() ? "ACTIONBAR" : "CHAT"));
        messages.sendRaw(sender, "<gray>Social event prefix:</gray> <white><state></white>", Map.of("state", cfg.feedback().socialEventPrefix() ? "ENABLED" : "DISABLED"));
        messages.sendRaw(sender, "<gray>AFK short-return suppression:</gray> <white><seconds>s</white>", Map.of("seconds", cfg.feedback().afkSuppressShortReturnNanos() / 1_000_000_000L));
        messages.sendRaw(sender, "<gray>AFK state persistence:</gray> <white>EPHEMERAL</white>");
        messages.sendRaw(sender, "<gray>PlaceholderAPI:</gray> <white><state></white>", Map.of("state", plugin.placeholderRegistered() ? "REGISTERED" : "UNAVAILABLE"));
        messages.sendRaw(sender, "<gray>PlexonFamily integrations:</gray> <white><ready>/<total></white>", Map.of("ready", family.readyCount(), "total", family.totalCount()));
        messages.sendRaw(sender, "<gray>PlexonHomes:</gray> <white><state></white>", Map.of("state", family.ready("PLEXON_HOMES") ? "READY" : "MISSING"));
        if (core != null) {
            var snapshot = core.diagnostics();
            messages.sendRaw(sender, "<gray>Core health:</gray> <white><health></white>", Map.of("health", snapshot.health()));
            messages.sendRaw(sender, "<gray>Core GUI sessions:</gray> <white><sessions></white>", Map.of("sessions", core.gui().activeSessions()));
            messages.sendRaw(sender, "<gray>Core compute/IO queues:</gray> <white><compute>/<io></white>", Map.of("compute", core.scheduler().computeQueueSize(), "io", core.scheduler().ioQueueSize()));
        }
    }

    private void integrations(CommandSender sender) {
        var statuses = complements.refresh();
        messages.sendRaw(sender, "<gradient:#57E389:#22D3EE><bold>External Complements</bold></gradient> <dark_gray>— specialist systems are not reimplemented here.</dark_gray>");
        for (var status : statuses) {
            String state = status.detectedAny() ? "<green>READY</green>" : "<yellow>NOT DETECTED</yellow>";
            messages.sendRaw(sender, "<gray><category>:</gray> " + state + " <dark_gray>•</dark_gray> <white><providers></white>", Map.of(
                    "category", status.category().label(),
                    "providers", status.providerSummary()));
        }
    }

    private void family(CommandSender sender) {
        var statuses = family.refresh();
        messages.sendRaw(sender, "<gradient:#57E389:#22D3EE><bold>PlexonFamily Compatibility</bold></gradient>");
        for (var status : statuses) {
            String state = status.ready() ? "<green>READY</green>" : "<dark_gray>NOT ACTIVE</dark_gray>";
            messages.sendRaw(sender, "<gray><plugin>:</gray> " + state + " <dark_gray>•</dark_gray> <white><version></white>", Map.of(
                    "plugin", status.pluginName(),
                    "version", status.version()));
        }
        messages.sendRaw(sender,
                "<gray>Homes limit contract:</gray> <white><numeric></white> <dark_gray>or</dark_gray> <white><unlimited></white>",
                Map.of("numeric", "plexonhomes.limit.<N>", "unlimited", "plexonhomes.limit.unlimited"));
    }
}
