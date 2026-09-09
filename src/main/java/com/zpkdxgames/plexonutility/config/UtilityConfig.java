package com.zpkdxgames.plexonutility.config;

import com.zpkdxgames.plexonutility.feature.Feature;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
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
        long healCooldownNanos,
        boolean claimStandardCommands) {

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
            if (config.getBoolean("features." + feature.id(), true)) enabled.add(feature);
        }

        int foodLevel = clamp(config.getInt("feed.food-level", 20), 0, 20);
        double configuredSaturation = config.getDouble("feed.saturation", 20.0D);
        float saturation = (float) Math.max(0.0D, Math.min(configuredSaturation, foodLevel));
        boolean resetExhaustion = config.getBoolean("feed.reset-exhaustion", true);
        long feedCooldown = secondsToNanos(config.getLong("feed.cooldown-seconds", 0L));

        boolean clearFire = config.getBoolean("heal.clear-fire", true);
        boolean clearNegative = config.getBoolean("heal.clear-negative-effects", false);
        Set<String> effects = new LinkedHashSet<>();
        for (String raw : config.getStringList("heal.negative-effects")) {
            if (raw == null || raw.isBlank()) continue;
            effects.add(raw.trim().toUpperCase(Locale.ROOT));
        }
        long healCooldown = secondsToNanos(config.getLong("heal.cooldown-seconds", 0L));

        return new UtilityConfig(
                enabled,
                foodLevel,
                saturation,
                resetExhaustion,
                feedCooldown,
                clearFire,
                clearNegative,
                effects,
                healCooldown,
                config.getBoolean("migration.claim-standard-commands", true));
    }

    public boolean enabled(Feature feature) {
        return enabledFeatures.contains(feature);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static long secondsToNanos(long seconds) {
        if (seconds <= 0L) return 0L;
        if (seconds > 86_400L) throw new IllegalArgumentException("Cooldown cannot exceed 86400 seconds");
        return Math.multiplyExact(seconds, 1_000_000_000L);
    }
}
