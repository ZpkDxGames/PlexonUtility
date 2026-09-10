package com.zpkdxgames.plexonutility.config;

import com.zpkdxgames.plexonutility.feature.Feature;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public record UtilityConfig(
        Set<Feature> enabledFeatures,
        int feedFoodLevel,
        float feedSaturation,
        boolean feedResetExhaustion,
        long feedCooldownNanos,
        boolean healClearFire,
        boolean healClearNegativeEffects,
        Set<String> healNegativeEffects,
        long healCooldownNanos) {

    public UtilityConfig {
        EnumSet<Feature> featureCopy = enabledFeatures.isEmpty()
                ? EnumSet.noneOf(Feature.class)
                : EnumSet.copyOf(enabledFeatures);
        enabledFeatures = Collections.unmodifiableSet(featureCopy);
        healNegativeEffects = Collections.unmodifiableSet(new LinkedHashSet<>(healNegativeEffects));
    }

    public static UtilityConfig from(FileConfiguration config) {
        EnumSet<Feature> enabled = EnumSet.noneOf(Feature.class);
        for (Feature feature : Feature.values()) {
            if (readBoolean(config, "features." + feature.id(), true)) enabled.add(feature);
        }

        int foodLevel = readInt(config, "feed.food-level", 20, 0, 20);
        double configuredSaturation = readDouble(config, "feed.saturation", 20.0D, 0.0D, 20.0D);
        if (configuredSaturation > foodLevel) {
            throw invalid("feed.saturation", "must not exceed feed.food-level");
        }
        boolean resetExhaustion = readBoolean(config, "feed.reset-exhaustion", true);
        long feedCooldown = secondsToNanos(readLong(config, "feed.cooldown-seconds", 0L, 0L, 86_400L));

        boolean clearFire = readBoolean(config, "heal.clear-fire", true);
        boolean clearNegative = readBoolean(config, "heal.clear-negative-effects", false);
        Set<String> effects = readStringSet(config, "heal.negative-effects");
        long healCooldown = secondsToNanos(readLong(config, "heal.cooldown-seconds", 0L, 0L, 86_400L));

        return new UtilityConfig(
                enabled,
                foodLevel,
                (float) configuredSaturation,
                resetExhaustion,
                feedCooldown,
                clearFire,
                clearNegative,
                effects,
                healCooldown);
    }

    public boolean enabled(Feature feature) {
        return enabledFeatures.contains(feature);
    }

    private static boolean readBoolean(FileConfiguration config, String path, boolean fallback) {
        Object raw = config.get(path);
        if (raw == null) return fallback;
        if (raw instanceof Boolean value) return value;
        throw invalid(path, "must be a boolean");
    }

    private static int readInt(FileConfiguration config, String path, int fallback, int min, int max) {
        Object raw = config.get(path);
        if (raw == null) return fallback;
        if (!(raw instanceof Number number)) throw invalid(path, "must be an integer");
        double value = number.doubleValue();
        if (!Double.isFinite(value) || Math.rint(value) != value || value < min || value > max) {
            throw invalid(path, "must be an integer from " + min + " to " + max);
        }
        return (int) value;
    }

    private static long readLong(FileConfiguration config, String path, long fallback, long min, long max) {
        Object raw = config.get(path);
        if (raw == null) return fallback;
        if (!(raw instanceof Number number)) throw invalid(path, "must be an integer");
        double value = number.doubleValue();
        if (!Double.isFinite(value) || Math.rint(value) != value || value < min || value > max) {
            throw invalid(path, "must be an integer from " + min + " to " + max);
        }
        return number.longValue();
    }

    private static double readDouble(FileConfiguration config, String path, double fallback, double min, double max) {
        Object raw = config.get(path);
        if (raw == null) return fallback;
        if (!(raw instanceof Number number)) throw invalid(path, "must be numeric");
        double value = number.doubleValue();
        if (!Double.isFinite(value) || value < min || value > max) {
            throw invalid(path, "must be from " + min + " to " + max);
        }
        return value;
    }

    private static Set<String> readStringSet(FileConfiguration config, String path) {
        Object raw = config.get(path);
        if (raw == null) return Set.of();
        if (!(raw instanceof List<?> list)) throw invalid(path, "must be a string list");

        Set<String> values = new LinkedHashSet<>();
        for (Object item : list) {
            if (!(item instanceof String text) || text.isBlank()) {
                throw invalid(path, "must contain only non-blank strings");
            }
            values.add(text.trim().toUpperCase(Locale.ROOT));
        }
        return values;
    }

    private static long secondsToNanos(long seconds) {
        return Math.multiplyExact(seconds, 1_000_000_000L);
    }

    private static IllegalArgumentException invalid(String path, String detail) {
        return new IllegalArgumentException(path + " " + detail);
    }
}
