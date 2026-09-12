package com.zpkdxgames.plexonutility.admin.vanish;

import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.feature.Feature;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VanishListenerTest {
    @Test void suppressionHidesJoinAndQuitAnnouncementsAndStillReconcilesJoin() {
        VanishService vanish = mock(VanishService.class);
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(vanish.isVanished(id)).thenReturn(true);
        PlayerJoinEvent join = mock(PlayerJoinEvent.class);
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(join.getPlayer()).thenReturn(player);
        when(quit.getPlayer()).thenReturn(player);
        VanishListener listener = new VanishListener(() -> config(true), vanish);

        listener.onJoin(join);
        listener.onQuit(quit);

        verify(join).joinMessage(null);
        verify(quit).quitMessage(null);
        verify(vanish).onJoin(player);
    }

    @Test void suppressionPolicyOffPreservesAnnouncements() {
        VanishService vanish = mock(VanishService.class);
        Player player = mock(Player.class);
        UUID id = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(id);
        when(vanish.isVanished(id)).thenReturn(true);
        PlayerJoinEvent join = mock(PlayerJoinEvent.class);
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class);
        when(join.getPlayer()).thenReturn(player);
        when(quit.getPlayer()).thenReturn(player);
        VanishListener listener = new VanishListener(() -> config(false), vanish);

        listener.onJoin(join);
        listener.onQuit(quit);

        verify(join, never()).joinMessage(nullable(Component.class));
        verify(quit, never()).quitMessage(nullable(Component.class));
        verify(vanish).onJoin(player);
    }

    private static UtilityConfig config(boolean suppressJoinQuit) {
        return new UtilityConfig(
                EnumSet.allOf(Feature.class), 20, 20.0F, true, 0L,
                true, false, Set.of(), 0L,
                UtilityConfig.AfkConfig.defaults(), UtilityConfig.FeedbackConfig.defaults(),
                new UtilityConfig.AdminConfig(true,
                        new UtilityConfig.VanishConfig(true, true, suppressJoinQuit),
                        UtilityConfig.ModerationConfig.defaults(), true, true));
    }
}
