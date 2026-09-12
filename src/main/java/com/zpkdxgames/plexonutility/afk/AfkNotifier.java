package com.zpkdxgames.plexonutility.afk;

import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.feedback.FeedbackService;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

final class AfkNotifier {
    private final Supplier<UtilityConfig> config;
    private final FeedbackService feedback;

    AfkNotifier(Supplier<UtilityConfig> config, FeedbackService feedback) {
        this.config = config;
        this.feedback = feedback;
    }

    void notify(Player player, AfkTracker.Transition transition) {
        if (transition == AfkTracker.Transition.NONE) return;
        boolean announce = config.get().afk().announcementsEnabled();
        if (transition == AfkTracker.Transition.TO_AFK) feedback.afkEntered(player, announce);
        else feedback.afkExited(player, announce);
    }

    void refresh(Player player, boolean afk) {
        feedback.refreshAfkState(player, afk);
    }

    void clear(Player player) {
        feedback.clearPlayer(player);
    }
}
