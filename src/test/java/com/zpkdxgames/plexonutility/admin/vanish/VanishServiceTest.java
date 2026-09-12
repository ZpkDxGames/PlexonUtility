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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
        UtilityConfig config = config(false, true, true);

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
        UtilityConfig config = config(false, true, true);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(target, viewer));
            VanishService service = new VanishService(plugin, () -> config, data, mock(AdminAuditService.class));
            service.setVanished(target, true, target);

            verify(viewer).showPlayer(plugin, target);
            verify(viewer).listPlayer(target);
            verify(viewer, never()).hidePlayer(plugin, target);
        }
    }

    @Test void persistenceWritesOnlyForRealStateChangesAndToggleOffRestoresVisibility() {
        Plugin plugin = mock(Plugin.class);
        Player target = player("Staff");
        Player viewer = player("Viewer");
        when(viewer.canSee(target)).thenReturn(true);
        AdminDataStore data = mock(AdminDataStore.class);
        when(data.snapshot()).thenReturn(AdminDataStore.Snapshot.empty());
        UtilityConfig config = config(true, true, true);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(target, viewer));
            VanishService service = new VanishService(plugin, () -> config, data, mock(AdminAuditService.class));
            service.setVanished(target, true, target);
            service.setVanished(target, true, target);
            assertTrue(service.isVanished(target.getUniqueId()));
            verify(data, times(1)).setVanished(target.getUniqueId(), true);

            assertFalse(service.toggle(target, target));
            assertFalse(service.isVanished(target.getUniqueId()));
            verify(data, times(1)).setVanished(target.getUniqueId(), false);
            verify(viewer).showPlayer(plugin, target);
        }
    }

    @Test void joiningViewerReceivesCurrentVanishState() {
        Plugin plugin = mock(Plugin.class);
        Player target = player("Staff");
        Player joining = player("JoiningViewer");
        when(target.isOnline()).thenReturn(true);
        AdminDataStore data = mock(AdminDataStore.class);
        when(data.snapshot()).thenReturn(AdminDataStore.Snapshot.empty());
        UtilityConfig config = config(false, true, true);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(target));
            VanishService service = new VanishService(plugin, () -> config, data, mock(AdminAuditService.class));
            service.setVanished(target, true, target);
            bukkit.when(() -> Bukkit.getPlayer(target.getUniqueId())).thenReturn(target);

            service.onJoin(joining);

            verify(joining).hidePlayer(plugin, target);
            verify(joining).unlistPlayer(target);
        }
    }

    @Test void disablingVanishOnReloadRestoresOwnedVisibilityAndClearsState() {
        Plugin plugin = mock(Plugin.class);
        Player target = player("Staff");
        Player viewer = player("Viewer");
        when(viewer.canSee(target)).thenReturn(true);
        AdminDataStore data = mock(AdminDataStore.class);
        when(data.snapshot()).thenReturn(AdminDataStore.Snapshot.empty());
        AtomicReference<UtilityConfig> config = new AtomicReference<>(config(false, true, true));

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(target, viewer));
            bukkit.when(() -> Bukkit.getPlayer(target.getUniqueId())).thenReturn(target);
            VanishService service = new VanishService(plugin, config::get, data, mock(AdminAuditService.class));
            service.setVanished(target, true, target);
            config.set(config(false, true, false));

            service.reload();

            assertFalse(service.isVanished(target.getUniqueId()));
            verify(viewer).showPlayer(plugin, target);
            verify(viewer).listPlayer(target);
        }
    }

    private static Player player(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return player;
    }

    private static UtilityConfig config(boolean persist, boolean adminEnabled, boolean vanishEnabled) {
        return new UtilityConfig(
                EnumSet.allOf(Feature.class), 20, 20.0F, true, 0L,
                true, false, Set.of(), 0L,
                UtilityConfig.AfkConfig.defaults(), UtilityConfig.FeedbackConfig.defaults(),
                new UtilityConfig.AdminConfig(adminEnabled,
                        new UtilityConfig.VanishConfig(vanishEnabled, persist, true),
                        UtilityConfig.ModerationConfig.defaults(), true, true));
    }
}
