package com.zpkdxgames.plexonutility.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilityAdminConfigTest {
    @Test void adminDefaultsAreStable() {
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());
        assertTrue(config.admin().enabled());
        assertTrue(config.admin().vanish().enabled());
        assertTrue(config.admin().vanish().persist());
        assertTrue(config.admin().vanish().suppressJoinQuit());
        assertTrue(config.admin().moderation().kickEnabled());
        assertTrue(config.admin().moderation().banEnabled());
        assertEquals(3650, config.admin().moderation().maxDurationDays());
        assertTrue(config.admin().prisonEnabled());
        assertTrue(config.admin().inventoryInspectionEnabled());
    }

    @Test void configuredAdminPolicyIsParsed() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("admin.enabled", false);
        yaml.set("admin.vanish.persist", false);
        yaml.set("admin.moderation.ban.max-duration-days", 30);
        UtilityConfig config = UtilityConfig.from(yaml);
        assertFalse(config.admin().enabled());
        assertFalse(config.admin().vanish().persist());
        assertEquals(30, config.admin().moderation().maxDurationDays());
    }

    @Test void invalidModerationBoundsAndMultilineDefaultsAreRejected() {
        YamlConfiguration max = new YamlConfiguration();
        max.set("admin.moderation.ban.max-duration-days", 0);
        assertThrows(IllegalArgumentException.class, () -> UtilityConfig.from(max));

        YamlConfiguration reason = new YamlConfiguration();
        reason.set("admin.moderation.kick.default-reason", "bad\nreason");
        assertThrows(IllegalArgumentException.class, () -> UtilityConfig.from(reason));
    }
}
