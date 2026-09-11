package com.zpkdxgames.plexonutility.config;

import com.zpkdxgames.plexonutility.feature.Feature;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilityConfigTest {
    @Test
    void acceptsSafeDefaultsIncludingAfkMigrationDefaults() {
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());

        assertEquals(20, config.feedFoodLevel());
        assertEquals(20.0F, config.feedSaturation());
        assertEquals(0L, config.feedCooldownNanos());
        assertEquals(0L, config.healCooldownNanos());
        assertTrue(config.enabledFeatures().containsAll(List.of(
                Feature.FEED, Feature.HEAL, Feature.ENDERCHEST, Feature.WORKBENCH, Feature.AFK)));
        assertTrue(config.afk().autoTimeoutEnabled());
        assertEquals(300_000_000_000L, config.afk().timeoutNanos());
        assertEquals(200L, config.afk().scanIntervalTicks());
        assertTrue(config.afk().announcementsEnabled());
        assertEquals("", config.afk().placeholderActive());
        assertEquals(" <gray>[AFK]</gray>", config.afk().placeholderAfk());
    }

    @Test
    void afkAutoTimeoutCanBeDisabledWithoutDisablingManualAfk() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("afk.auto-timeout.enabled", false);

        UtilityConfig config = UtilityConfig.from(yaml);

        assertTrue(config.enabled(Feature.AFK));
        assertFalse(config.afk().autoTimeoutEnabled());
    }

    @Test
    void afkAnnouncementsCanBeDisabled() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("afk.announcements.enabled", false);
        assertFalse(UtilityConfig.from(yaml).afk().announcementsEnabled());
    }

    @Test
    void rejectsUnsafeAfkTimeout() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("afk.auto-timeout.seconds", 0);
        assertThrows(IllegalArgumentException.class, () -> UtilityConfig.from(yaml));
    }

    @Test
    void rejectsMultilineAfkPlaceholder() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("afk.placeholder.afk", "AFK\nBAD");
        assertThrows(IllegalArgumentException.class, () -> UtilityConfig.from(yaml));
    }

    @Test
    void rejectsOutOfRangeFoodInsteadOfSilentlyClamping() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("feed.food-level", 99);

        assertThrows(IllegalArgumentException.class, () -> UtilityConfig.from(yaml));
    }

    @Test
    void rejectsSaturationAboveConfiguredFoodLevel() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("feed.food-level", 10);
        yaml.set("feed.saturation", 11.0D);

        assertThrows(IllegalArgumentException.class, () -> UtilityConfig.from(yaml));
    }

    @Test
    void rejectsWrongBooleanType() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("heal.clear-fire", "yes");

        assertThrows(IllegalArgumentException.class, () -> UtilityConfig.from(yaml));
    }

    @Test
    void rejectsFractionalCooldown() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("feed.cooldown-seconds", 1.5D);

        assertThrows(IllegalArgumentException.class, () -> UtilityConfig.from(yaml));
    }

    @Test
    void rejectsUnreasonableCooldown() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("feed.cooldown-seconds", 86_401L);

        assertThrows(IllegalArgumentException.class, () -> UtilityConfig.from(yaml));
    }

    @Test
    void rejectsNonStringNegativeEffectEntries() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("heal.negative-effects", List.of("POISON", 12));

        assertThrows(IllegalArgumentException.class, () -> UtilityConfig.from(yaml));
    }

    @Test
    void allowsFeatureToBeDisabledIndependently() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("features.heal", false);

        UtilityConfig config = UtilityConfig.from(yaml);

        assertFalse(config.enabled(Feature.HEAL));
        assertTrue(config.enabled(Feature.FEED));
        assertTrue(config.enabled(Feature.ENDERCHEST));
        assertTrue(config.enabled(Feature.WORKBENCH));
        assertTrue(config.enabled(Feature.AFK));
    }

    @Test
    void allowsEveryFeatureToBeDisabled() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Feature feature : Feature.values()) yaml.set("features." + feature.id(), false);

        UtilityConfig config = UtilityConfig.from(yaml);

        assertTrue(config.enabledFeatures().isEmpty());
    }
}
