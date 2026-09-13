package com.zpkdxgames.plexonutility.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Utility350ConfigTest {
    @Test void additive350DefaultsLoadFromLegacyStyleConfig() {
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());
        assertTrue(config.admin().entityManagement().enabled());
        assertTrue(config.admin().entityManagement().killall().enabled());
        assertEquals(512, config.admin().entityManagement().killall().maxRadius());
        assertEquals(250, config.admin().entityManagement().killall().confirmationThreshold());
        assertEquals(100, config.admin().entityManagement().spawnmob().maxAmount());
        assertTrue(config.admin().playerManagement().gamemodeEnabled());
        assertTrue(config.admin().playerManagement().anvilEnabled());
        assertFalse(config.admin().playerManagement().godPersist());
        assertTrue(config.admin().vanish().syntheticPresence().enabled());
        assertEquals("ORDINARY_PLAYERS", config.admin().vanish().syntheticPresence().audience());
    }

    @Test void spawnAmountCanOnlyLowerHardCeiling() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("admin.entity-management.spawnmob.max-amount", 101);
        assertThrows(IllegalArgumentException.class, () -> UtilityConfig.from(yaml));
    }

    @Test void configuredLowerSpawnMaximumIsAccepted() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("admin.entity-management.spawnmob.max-amount", 25);
        assertEquals(25, UtilityConfig.from(yaml).admin().entityManagement().spawnmob().maxAmount());
    }

    @Test void invalidCleanupRadiusIsRejected() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("admin.entity-management.killall.max-radius", -1);
        assertThrows(IllegalArgumentException.class, () -> UtilityConfig.from(yaml));
    }

    @Test void invalidSyntheticAudienceIsRejected() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("admin.vanish.synthetic-presence.audience", "EVERYONE_AND_CONSOLE");
        assertThrows(IllegalArgumentException.class, () -> UtilityConfig.from(yaml));
    }

    @Test void blockedEntityTypesNormalizeForRegistryComparison() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("admin.entity-management.spawnmob.blocked-entity-types", java.util.List.of("zombie-villager"));
        assertTrue(UtilityConfig.from(yaml).admin().entityManagement().spawnmob().blockedEntityTypes().contains("ZOMBIE_VILLAGER"));
    }
}
