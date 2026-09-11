package com.zpkdxgames.plexonutility.afk;

import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.function.Supplier;

final class AfkNotifier {
    private final Supplier<UtilityConfig> config;
    private final MessageService messages;

    AfkNotifier(Supplier<UtilityConfig> config, MessageService messages) {
        this.config = config;
        this.messages = messages;
    }

    void notify(Player player, AfkTracker.Transition transition, boolean directFeedback) {
        if (transition == AfkTracker.Transition.NONE) return;

        if (directFeedback || transition == AfkTracker.Transition.TO_ACTIVE) {
            messages.send(player, transition == AfkTracker.Transition.TO_AFK ? "afk-self-on" : "afk-self-off");
        }

        if (config.get().afk().announcementsEnabled()) {
            messages.broadcast(
                    transition == AfkTracker.Transition.TO_AFK ? "afk-announcement-on" : "afk-announcement-off",
                    Map.of("player", player.getName()));
        }
    }
}
