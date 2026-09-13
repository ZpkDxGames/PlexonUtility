package com.zpkdxgames.plexonutility.admin.vanish;

import com.zpkdxgames.plexoncore.text.TextService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Presentation-only synthetic presence. It never dispatches Bukkit join/quit lifecycle events. */
public final class SyntheticPresenceBridge {
    private final Plugin plugin;
    private final Supplier<UtilityConfig> config;
    private final TextService text;

    public SyntheticPresenceBridge(Plugin plugin, Supplier<UtilityConfig> config, TextService text) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.config = Objects.requireNonNull(config, "config");
        this.text = Objects.requireNonNull(text, "text");
    }

    public BroadcastResult broadcast(Player actor, boolean vanished) {
        UtilityConfig.SyntheticPresenceConfig policy = config.get().admin().vanish().syntheticPresence();
        if (!policy.enabled()) return new BroadcastResult(0, "disabled");

        String template = vanished ? policy.quitTemplate() : policy.joinTemplate();
        Component rendered = text.renderTemplate(template, Map.of("player", actor.getName()));
        boolean ordinaryOnly = policy.audience().equals("ORDINARY_PLAYERS");
        int recipients = 0;
        for (Player viewer : plugin.getServer().getOnlinePlayers()) {
            if (viewer.getUniqueId().equals(actor.getUniqueId())) continue;
            if (ordinaryOnly && viewer.hasPermission("plexonutility.admin.vanish.see")) continue;
            viewer.sendMessage(rendered);
            recipients++;
        }

        boolean chatsPresent = policy.preferPlexonChats()
                && plugin.getServer().getPluginManager().getPlugin("PlexonChats") != null;
        return new BroadcastResult(recipients,
                chatsPresent ? "utility-fallback-plexonchats-api-unavailable" : "utility-fallback");
    }

    public String mode() {
        UtilityConfig.SyntheticPresenceConfig policy = config.get().admin().vanish().syntheticPresence();
        if (!policy.enabled()) return "disabled";
        if (policy.preferPlexonChats() && plugin.getServer().getPluginManager().getPlugin("PlexonChats") != null) {
            return "utility-fallback-plexonchats-api-unavailable";
        }
        return "utility-fallback";
    }

    public record BroadcastResult(int recipients, String mode) { }
}
