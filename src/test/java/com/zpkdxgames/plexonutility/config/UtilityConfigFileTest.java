package com.zpkdxgames.plexonutility.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilityConfigFileTest {
    @TempDir Path tempDir;

    @Test
    void legacyConfigMigratesToCurrentSchemaAndPreservesCustomValues() throws Exception {
        Path file = tempDir.resolve("config.yml");
        Files.writeString(file, "features:\n  feed: false\n");
        YamlConfiguration defaults = defaults();

        UtilityConfigFile.Candidate candidate = UtilityConfigFile.prepare(file.toFile(), defaults);

        assertEquals(0, candidate.sourceSchema());
        assertEquals(UtilityConfigFile.CURRENT_SCHEMA, candidate.yaml().getInt("schema-version"));
        assertFalse(candidate.runtime().enabled(com.zpkdxgames.plexonutility.feature.Feature.FEED));
        assertTrue(candidate.migrated() > 0);
        assertEquals("default-value", candidate.yaml().getString("migration.sample"));
    }

    @Test
    void futureSchemaIsRefused() throws Exception {
        Path file = tempDir.resolve("config.yml");
        Files.writeString(file, "schema-version: 99\n");

        assertThrows(IllegalArgumentException.class,
                () -> UtilityConfigFile.prepare(file.toFile(), defaults()));
    }

    @Test
    void currentCompleteSchemaRequiresNoRewrite() throws Exception {
        Path file = tempDir.resolve("config.yml");
        YamlConfiguration defaults = defaults();
        defaults.save(file.toFile());

        UtilityConfigFile.Candidate candidate = UtilityConfigFile.prepare(file.toFile(), defaults);

        assertEquals(UtilityConfigFile.CURRENT_SCHEMA, candidate.sourceSchema());
        assertEquals(0, candidate.migrated());
    }

    private static YamlConfiguration defaults() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("schema-version", UtilityConfigFile.CURRENT_SCHEMA);
        yaml.set("features.feed", true);
        yaml.set("migration.sample", "default-value");
        return yaml;
    }
}
