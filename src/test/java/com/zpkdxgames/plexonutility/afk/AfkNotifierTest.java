package com.zpkdxgames.plexonutility.afk;

import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AfkNotifierTest {
    @Test
    void transitionBroadcastsWhenAnnouncementsAreEnabled() {
        MessageService messages = mock(MessageService.class);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alex");
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());
        AfkNotifier notifier = new AfkNotifier(() -> config, messages);

        notifier.notify(player, AfkTracker.Transition.TO_AFK, true);

        verify(messages).send(player, "afk-self-on");
        verify(messages).broadcast("afk-announcement-on", Map.of("player", "Alex"));
    }

    @Test
    void transitionDoesNotBroadcastWhenAnnouncementsAreDisabled() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("afk.announcements.enabled", false);
        UtilityConfig config = UtilityConfig.from(yaml);
        MessageService messages = mock(MessageService.class);
        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Alex");
        AfkNotifier notifier = new AfkNotifier(() -> config, messages);

        notifier.notify(player, AfkTracker.Transition.TO_ACTIVE, false);

        verify(messages).send(player, "afk-self-off");
        verify(messages, never()).broadcast("afk-announcement-off", Map.of("player", "Alex"));
    }
}
