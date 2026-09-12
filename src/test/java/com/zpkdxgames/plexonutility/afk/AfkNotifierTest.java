package com.zpkdxgames.plexonutility.afk;

import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.feedback.FeedbackService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AfkNotifierTest {
    @Test
    void transitionDelegatesAfkEntryWithAnnouncementPolicy() {
        FeedbackService feedback = mock(FeedbackService.class);
        Player player = mock(Player.class);
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());
        AfkNotifier notifier = new AfkNotifier(() -> config, feedback);

        notifier.notify(player, AfkTracker.Transition.TO_AFK);

        verify(feedback).afkEntered(player, true);
    }

    @Test
    void transitionDelegatesActiveStateWithoutAnnouncementWhenDisabled() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("afk.announcements.enabled", false);
        UtilityConfig config = UtilityConfig.from(yaml);
        FeedbackService feedback = mock(FeedbackService.class);
        Player player = mock(Player.class);
        AfkNotifier notifier = new AfkNotifier(() -> config, feedback);

        notifier.notify(player, AfkTracker.Transition.TO_ACTIVE);

        verify(feedback).afkExited(player, false);
    }
}
