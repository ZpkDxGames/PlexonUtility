package com.zpkdxgames.plexonutility.admin.vanish;

import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.admin.AdminDataStore;
import com.zpkdxgames.plexonutility.api.event.VanishStateChangeEvent;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VanishSyntheticTransitionTest {
    @Test void syntheticPresenceAndEventOnlyOccurOnRealTransition() {
        Plugin plugin = mock(Plugin.class);
        Player target = player("Staff");
        Player viewer = player("Viewer");
        AdminDataStore data = mock(AdminDataStore.class);
        when(data.snapshot()).thenReturn(AdminDataStore.Snapshot.empty());
        SyntheticPresenceBridge bridge = mock(SyntheticPresenceBridge.class);
        when(bridge.broadcast(target, true)).thenReturn(new SyntheticPresenceBridge.BroadcastResult(1, "utility-fallback"));
        PluginManager pluginManager = mock(PluginManager.class);
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(target, viewer));
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            VanishService service = new VanishService(plugin, () -> config, data, mock(AdminAuditService.class), bridge);

            assertTrue(service.setVanished(target, true, target));
            assertTrue(service.setVanished(target, true, target));

            verify(bridge, times(1)).broadcast(target, true);
            verify(pluginManager, times(1)).callEvent(any(VanishStateChangeEvent.class));
            ArgumentCaptor<VanishStateChangeEvent> event = ArgumentCaptor.forClass(VanishStateChangeEvent.class);
            verify(pluginManager).callEvent(event.capture());
            assertTrue(event.getValue().vanished());
            assertTrue(event.getValue().syntheticPresenceRequested());
        }
    }

    private static Player player(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.nameUUIDFromBytes(name.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return player;
    }
}
