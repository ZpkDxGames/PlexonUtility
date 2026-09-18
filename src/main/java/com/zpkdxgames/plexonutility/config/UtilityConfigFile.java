package com.zpkdxgames.plexonutility.config;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Formal config.yml schema preparation. Candidate preparation is strictly side-effect-free. */
public final class UtilityConfigFile {
    public static final int CURRENT_SCHEMA = 1;

    private UtilityConfigFile() { }

    public static Candidate prepare(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "config.yml");
        YamlConfiguration defaults = loadBundledDefaults(plugin);
        return prepare(file, defaults);
    }

    static Candidate prepare(File file, YamlConfiguration defaults) {
        int bundledSchema = schema(defaults, true);
        if (bundledSchema != CURRENT_SCHEMA) {
            throw new IllegalStateException("Bundled config.yml schema-version must be " + CURRENT_SCHEMA);
        }

        boolean existed = file.isFile();
        YamlConfiguration candidate = new YamlConfiguration();
        if (existed) {
            try {
                candidate.load(file);
            } catch (IOException | InvalidConfigurationException exception) {
                throw new IllegalArgumentException("config.yml could not be loaded: " + exception.getMessage(), exception);
            }
        }

        int sourceSchema = schema(candidate, false);
        if (sourceSchema > CURRENT_SCHEMA) {
            throw new IllegalArgumentException("config.yml schema-version " + sourceSchema
                    + " is newer than supported schema " + CURRENT_SCHEMA);
        }

        int migrated = 0;
        for (Map.Entry<String, Object> entry : defaults.getValues(true).entrySet()) {
            String path = entry.getKey();
            Object bundled = entry.getValue();
            if (bundled instanceof org.bukkit.configuration.ConfigurationSection) continue;
            if (candidate.get(path) == null) {
                candidate.set(path, bundled);
                migrated++;
            }
        }
        if (sourceSchema != CURRENT_SCHEMA) {
            candidate.set("schema-version", CURRENT_SCHEMA);
            if (!defaults.contains("schema-version")) throw new IllegalStateException("Bundled config schema missing");
            if (sourceSchema != 0 || !existed) {
                // schema-version may already have been counted as a missing default above.
            }
        }

        // Count is informational; guarantee at least one migration for a legacy existing file.
        if (existed && sourceSchema == 0 && migrated == 0) migrated = 1;

        UtilityConfig parsed = UtilityConfig.from(candidate);
        return new Candidate(candidate, parsed, migrated, existed, sourceSchema);
    }

    private static int schema(YamlConfiguration yaml, boolean required) {
        Object raw = yaml.get("schema-version");
        if (raw == null) {
            if (required) throw new IllegalArgumentException("schema-version is required");
            return 0;
        }
        if (!(raw instanceof Number number)) {
            throw new IllegalArgumentException("config.yml schema-version must be an integer");
        }
        double value = number.doubleValue();
        if (!Double.isFinite(value) || Math.rint(value) != value || value < 0 || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("config.yml schema-version must be a non-negative integer");
        }
        return number.intValue();
    }

    private static YamlConfiguration loadBundledDefaults(JavaPlugin plugin) {
        try (InputStream stream = plugin.getResource("config.yml")) {
            if (stream == null) throw new IllegalStateException("Bundled config.yml is missing");
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Bundled config.yml could not be read", exception);
        }
    }

    public record Candidate(
            YamlConfiguration yaml,
            UtilityConfig runtime,
            int migrated,
            boolean sourceExisted,
            int sourceSchema) {
        public Candidate {
            if (yaml == null || runtime == null) throw new IllegalArgumentException("candidate");
            if (migrated < 0 || sourceSchema < 0) throw new IllegalArgumentException("migration metadata");
        }
    }
}
