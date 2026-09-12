package com.zpkdxgames.plexonutility.admin.vanish;

import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.admin.AdminDataStore;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.feature.Feature;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VanishServiceTest {
    @Test void unauthorizedViewerIsHiddenAndUnlistedWithoutScheduler() {
        Plugin plugin = mock(Plugin.class);
        Player target = player("Staff");
        Player viewer = player("Viewer");
        AdminDataStore data = mock(AdminDataStore.class);
        when(data.snapshot()).thenReturn(AdminDataStore.Snapshot.empty());
        AdminAuditService audit = mock(AdminAuditService.class);
        UtilityConfig config = config(false);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(target, viewer));
            VanishService service = new VanishService(plugin, () -> config, data, audit);
            assertFalse(service.isVanished(target.getUniqueId()));
            assertTrue(service.setVanished(target, true, target));

            verify(viewer).hidePlayer(plugin, target);
            verify(viewer).unlistPlayer(target);
            verify(viewer, never()).showPlayer(plugin, target);
        }
    }

    @Test void authorizedViewerKeepsVisibility() {
        Plugin plugin = mock(Plugin.class);
        Player target = player("Staff");
        Player viewer = player("SeniorStaff");
        when(viewer.hasPermission("plexonutility.admin.vanish.see")).thenReturn(true);
        when(viewer.canSee(target)).thenReturn(true);
        AdminDataStore data = mock(AdminDataStore.class);
        when(data.snapshot()).thenReturn(AdminDataStore.Snapshot.empty());
        UtilityConfig config = config(false);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(target, viewer));
            VanishService service = new VanishService(plugin, () -> config, data, mock(AdminAuditService.class));
            service.setVanished(target, true, target);

            verify(viewer).showPlayer(plugin, target);
            verify(viewer).listPlayer(target);
            verify(viewer, never()).hidePlayer(plugin, target);
        }
    }

    @Test void persistenceOnlyWritesOnConfiguredStateChange() {
        Plugin plugin = mock(Plugin.class);
        Player target = player("Staff");
        AdminDataStore data = mock(AdminDataStore.class);
        when(data.snapshot()).thenReturn(AdminDataStore.Snapshot.empty());
        UtilityConfig config = config(true);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(target));
            VanishService service = new VanishService(plugin, () -> config, data, mock(AdminAuditService.class));
            service.setVanished(target, true, target);
            assertTrue(service.isVanished(target.getUniqueId()));
            verify(data).setVanished(target.getUniqueId(), true);
        }
    }

    private static Player player(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return player;
    }

    private static UtilityConfig config(boolean persist) {
        return new UtilityConfig(
                EnumSet.allOf(Feature.class), 20, 20.0F, true, 0L,
                true, false, Set.of(), 0L,
                UtilityConfig.AfkConfig.defaults(), UtilityConfig.FeedbackConfig.defaults(),
                new UtilityConfig.AdminConfig(true,
                        new UtilityConfig.VanishConfig(true, persist, true),
                        UtilityConfig.ModerationConfig.defaults(), true, true));
    }
}
