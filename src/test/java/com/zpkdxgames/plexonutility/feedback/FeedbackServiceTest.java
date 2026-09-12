package com.zpkdxgames.plexonutility.feedback;

import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.message.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeedbackServiceTest {
    @Test
    void routineSelfSuccessUsesActionbarWithoutPrefix() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        MessageService messages = mock(MessageService.class);
        Player player = player("Alex");
        Component rendered = Component.text("Health restored");
        when(messages.renderUnprefixed("heal-self", Map.of())).thenReturn(rendered);
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());
        FeedbackService feedback = new FeedbackService(plugin, () -> config, messages, () -> 0L);

        feedback.success(player, "heal-self");

        verify(player).sendActionBar(rendered);
        verify(messages, never()).send(player, "heal-self", Map.of());
    }

    @Test
    void afkSocialBroadcastExcludesSubjectAndSuppressesQuickReturn() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        Player subject = player("Alex");
        Player observer = player("Sam");
        doReturn(List.of(subject, observer)).when(server).getOnlinePlayers();

        MessageService messages = mock(MessageService.class);
        Component entered = Component.text("Alex is now AFK");
        Component returned = Component.text("Alex is back");
        Component active = Component.text("You are active again");
        when(messages.renderUnprefixed("afk-announcement-on", Map.of("player", "Alex"))).thenReturn(entered);
        when(messages.renderUnprefixed("afk-announcement-off", Map.of("player", "Alex"))).thenReturn(returned);
        when(messages.renderUnprefixed("afk-return-actionbar", Map.of())).thenReturn(active);

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("feedback.afk.bossbar.enabled", false);
        yaml.set("feedback.afk.suppress-short-return-seconds", 8);
        UtilityConfig config = UtilityConfig.from(yaml);
        AtomicLong clock = new AtomicLong(1_000_000_000L);
        FeedbackService feedback = new FeedbackService(plugin, () -> config, messages, clock::get);

        feedback.afkEntered(subject, true);
        clock.addAndGet(2_000_000_000L);
        feedback.afkExited(subject, true);

        verify(observer).sendMessage(entered);
        verify(subject, never()).sendMessage(entered);
        verify(observer, never()).sendMessage(returned);
        verify(subject).sendActionBar(active);
    }

    @Test
    void afkReturnBroadcastsAfterMinimumDuration() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        Player subject = player("Alex");
        Player observer = player("Sam");
        doReturn(List.of(subject, observer)).when(server).getOnlinePlayers();

        MessageService messages = mock(MessageService.class);
        Component entered = Component.text("Alex is now AFK");
        Component returned = Component.text("Alex is back");
        Component active = Component.text("You are active again");
        when(messages.renderUnprefixed("afk-announcement-on", Map.of("player", "Alex"))).thenReturn(entered);
        when(messages.renderUnprefixed("afk-announcement-off", Map.of("player", "Alex"))).thenReturn(returned);
        when(messages.renderUnprefixed("afk-return-actionbar", Map.of())).thenReturn(active);

        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("feedback.afk.bossbar.enabled", false);
        yaml.set("feedback.afk.suppress-short-return-seconds", 8);
        UtilityConfig config = UtilityConfig.from(yaml);
        AtomicLong clock = new AtomicLong(1_000_000_000L);
        FeedbackService feedback = new FeedbackService(plugin, () -> config, messages, clock::get);

        feedback.afkEntered(subject, true);
        clock.addAndGet(9_000_000_000L);
        feedback.afkExited(subject, true);

        verify(observer).sendMessage(returned);
        verify(subject, never()).sendMessage(returned);
    }

    private static Player player(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return player;
    }
}
