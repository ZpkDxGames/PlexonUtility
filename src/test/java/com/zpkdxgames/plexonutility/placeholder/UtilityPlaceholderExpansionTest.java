package com.zpkdxgames.plexonutility.placeholder;

import com.zpkdxgames.plexonutility.afk.AfkTracker;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UtilityPlaceholderExpansionTest {
    @Test
    void returnsConfiguredActiveAndAfkDisplayStates() {
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());
        AfkTracker tracker = new AfkTracker();
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(uuid);
        tracker.join(uuid);
        UtilityPlaceholderExpansion expansion = new UtilityPlaceholderExpansion("test", () -> config, tracker);

        assertEquals("", expansion.onPlaceholderRequest(player, "afk"));
        assertEquals("false", expansion.onPlaceholderRequest(player, "is_afk"));

        tracker.toggle(uuid);

        assertEquals(" <gray>[AFK]</gray>", expansion.onPlaceholderRequest(player, "afk"));
        assertEquals("true", expansion.onPlaceholderRequest(player, "is_afk"));
    }

    @Test
    void nullPlayerReturnsActiveSafeValues() {
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());
        UtilityPlaceholderExpansion expansion = new UtilityPlaceholderExpansion("test", () -> config, new AfkTracker());

        assertEquals("", expansion.onPlaceholderRequest(null, "afk"));
        assertEquals("false", expansion.onPlaceholderRequest(null, "is_afk"));
    }
}
