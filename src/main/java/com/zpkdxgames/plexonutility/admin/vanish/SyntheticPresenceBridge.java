package com.zpkdxgames.plexonutility.admin.vanish;

import com.zpkdxgames.plexoncore.text.TextService;
import com.zpkdxgames.plexonutility.api.event.SyntheticPresencePresentationEvent;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Presentation-only synthetic presence authority.
 *
 * <p>It never dispatches Bukkit join/quit lifecycle events. External presenters receive one
 * synchronous request first; Utility renders the fallback only when the request remains unhandled.</p>
 */
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

        SyntheticPresencePresentationEvent request = new SyntheticPresencePresentationEvent(
                actor,
                vanished
                        ? SyntheticPresencePresentationEvent.Transition.SYNTHETIC_QUIT
                        : SyntheticPresencePresentationEvent.Transition.SYNTHETIC_JOIN,
                audience(policy));
        plugin.getServer().getPluginManager().callEvent(request);
        if (request.handled()) return new BroadcastResult(0, "external-presenter");

        String template = vanished ? policy.quitTemplate() : policy.joinTemplate();
        Component rendered = text.renderTemplate(template, Map.of("player", actor.getName()));
        boolean ordinaryOnly = request.audienceContext()
                == SyntheticPresencePresentationEvent.AudienceContext.ORDINARY_PLAYERS;
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
                chatsPresent ? "utility-fallback-plexonchats-unhandled" : "utility-fallback");
    }

    public String mode() {
        UtilityConfig.SyntheticPresenceConfig policy = config.get().admin().vanish().syntheticPresence();
        if (!policy.enabled()) return "disabled";
        if (policy.preferPlexonChats() && plugin.getServer().getPluginManager().getPlugin("PlexonChats") != null) {
            return "presentation-hook-with-utility-fallback";
        }
        return "utility-fallback";
    }

    private static SyntheticPresencePresentationEvent.AudienceContext audience(
            UtilityConfig.SyntheticPresenceConfig policy) {
        return policy.audience().equals("ALL_EXCEPT_SELF")
                ? SyntheticPresencePresentationEvent.AudienceContext.ALL_EXCEPT_SELF
                : SyntheticPresencePresentationEvent.AudienceContext.ORDINARY_PLAYERS;
    }

    public record BroadcastResult(int recipients, String mode) { }
}
