package com.zpkdxgames.plexonutility.admin.vanish;

import com.zpkdxgames.plexoncore.text.TextService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import net.kyori.adventure.text.Component;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyntheticPresenceBridgeTest {
    @Test void ordinaryAudienceExcludesActorAndVanishAwareStaff() {
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(pluginManager);
        Player actor = player("HiddenStaff");
        Player ordinary = player("Ordinary");
        Player staff = player("SeniorStaff");
        when(staff.hasPermission("plexonutility.admin.vanish.see")).thenReturn(true);
        when(server.getOnlinePlayers()).thenReturn(List.of(actor, ordinary, staff));
        TextService text = mock(TextService.class);
        Component rendered = Component.text("left");
        when(text.renderTemplate(anyString(), anyMap())).thenReturn(rendered);
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());

        SyntheticPresenceBridge.BroadcastResult result = new SyntheticPresenceBridge(plugin, () -> config, text)
                .broadcast(actor, true);

        assertEquals(1, result.recipients());
        verify(ordinary).sendMessage(rendered);
        verify(actor, never()).sendMessage(rendered);
        verify(staff, never()).sendMessage(rendered);
    }

    @Test void allExceptSelfCanIncludeVanishAwareStaff() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("admin.vanish.synthetic-presence.audience", "ALL_EXCEPT_SELF");
        UtilityConfig config = UtilityConfig.from(yaml);
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        when(server.getPluginManager()).thenReturn(mock(PluginManager.class));
        Player actor = player("HiddenStaff");
        Player staff = player("SeniorStaff");
        when(staff.hasPermission("plexonutility.admin.vanish.see")).thenReturn(true);
        when(server.getOnlinePlayers()).thenReturn(List.of(actor, staff));
        TextService text = mock(TextService.class);
        Component rendered = Component.text("left");
        when(text.renderTemplate(anyString(), anyMap())).thenReturn(rendered);

        SyntheticPresenceBridge.BroadcastResult result = new SyntheticPresenceBridge(plugin, () -> config, text)
                .broadcast(actor, true);

        assertEquals(1, result.recipients());
        verify(staff).sendMessage(rendered);
    }

    @Test void disabledSyntheticPresenceSendsNothing() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("admin.vanish.synthetic-presence.enabled", false);
        UtilityConfig config = UtilityConfig.from(yaml);
        Plugin plugin = mock(Plugin.class);
        Server server = mock(Server.class);
        when(plugin.getServer()).thenReturn(server);
        Player actor = player("HiddenStaff");
        TextService text = mock(TextService.class);

        SyntheticPresenceBridge.BroadcastResult result = new SyntheticPresenceBridge(plugin, () -> config, text)
                .broadcast(actor, true);

        assertEquals(0, result.recipients());
        verify(server, never()).getOnlinePlayers();
    }

    private static Player player(String name) {
        Player player = mock(Player.class);
        when(player.getName()).thenReturn(name);
        when(player.getUniqueId()).thenReturn(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)));
        return player;
    }
}
