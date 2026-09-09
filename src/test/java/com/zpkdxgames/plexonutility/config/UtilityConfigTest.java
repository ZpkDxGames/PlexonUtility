package com.zpkdxgames.plexonutility.config;

import com.zpkdxgames.plexonutility.feature.Feature;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilityConfigTest {
    @Test
    void clampsFeedValuesToBukkitSafeRange() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("feed.food-level", 99);
        yaml.set("feed.saturation", 200.0D);

        UtilityConfig config = UtilityConfig.from(yaml);

        assertEquals(20, config.feedFoodLevel());
        assertEquals(20.0F, config.feedSaturation());
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
    }

    @Test
    void allowsEveryFeatureToBeDisabled() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Feature feature : Feature.values()) yaml.set("features." + feature.id(), false);

        UtilityConfig config = UtilityConfig.from(yaml);

        assertTrue(config.enabledFeatures().isEmpty());
    }

    @Test
    void rejectsUnreasonableCooldown() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("feed.cooldown-seconds", 86_401L);
        assertThrows(IllegalArgumentException.class, () -> UtilityConfig.from(yaml));
    }

    @Test
    void zeroCooldownIsDisabled() {
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());
        assertEquals(0L, config.feedCooldownNanos());
        assertEquals(0L, config.healCooldownNanos());
    }
}
