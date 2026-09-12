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
        long healCooldownNanos,
        AfkConfig afk,
        FeedbackConfig feedback) {

    private static final Set<String> BOSSBAR_COLORS = Set.of("PINK", "BLUE", "RED", "GREEN", "YELLOW", "PURPLE", "WHITE");
    private static final Set<String> BOSSBAR_OVERLAYS = Set.of("PROGRESS", "NOTCHED_6", "NOTCHED_10", "NOTCHED_12", "NOTCHED_20");

    public record AfkConfig(
            boolean autoTimeoutEnabled,
            long timeoutNanos,
            long scanIntervalTicks,
            boolean announcementsEnabled,
            boolean resetOnMovement,
            boolean resetOnChat,
            boolean resetOnCommand,
            boolean resetOnInteraction,
            boolean resetOnBlockChange,
            boolean resetOnDamage,
            String placeholderActive,
            String placeholderAfk) {

        public AfkConfig {
            if (timeoutNanos < 0L) throw new IllegalArgumentException("AFK timeout must not be negative");
            if (scanIntervalTicks <= 0L) throw new IllegalArgumentException("AFK scan interval must be positive");
            placeholderActive = validatePlaceholder("afk.placeholder.active", placeholderActive);
            placeholderAfk = validatePlaceholder("afk.placeholder.afk", placeholderAfk);
        }

        public static AfkConfig defaults() {
            return new AfkConfig(
                    true,
                    secondsToNanos(300L),
                    secondsToTicks(10L),
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    "",
                    " <gray>[AFK]</gray>");
        }
    }

    /**
     * Presentation policy for routine player feedback. Chat remains available for errors/admin output,
     * while successful self-actions and persistent states use HUD surfaces by default.
     */
    public record FeedbackConfig(
            boolean utilitySuccessActionbar,
            boolean socialEventPrefix,
            boolean afkBossbarEnabled,
            String afkBossbarColor,
            String afkBossbarOverlay,
            boolean afkReturnActionbarEnabled,
            long afkSuppressShortReturnNanos) {

        public FeedbackConfig {
            afkBossbarColor = normalizeEnum("feedback.afk.bossbar.color", afkBossbarColor, BOSSBAR_COLORS);
            afkBossbarOverlay = normalizeEnum("feedback.afk.bossbar.overlay", afkBossbarOverlay, BOSSBAR_OVERLAYS);
            if (afkSuppressShortReturnNanos < 0L) {
                throw new IllegalArgumentException("feedback.afk.suppress-short-return-seconds must not be negative");
            }
        }

        public static FeedbackConfig defaults() {
            return new FeedbackConfig(true, false, true, "YELLOW", "PROGRESS", true, secondsToNanos(8L));
        }
    }

    public UtilityConfig {
        EnumSet<Feature> featureCopy = enabledFeatures.isEmpty()
                ? EnumSet.noneOf(Feature.class)
                : EnumSet.copyOf(enabledFeatures);
        enabledFeatures = Collections.unmodifiableSet(featureCopy);
        healNegativeEffects = Collections.unmodifiableSet(new LinkedHashSet<>(healNegativeEffects));
        if (afk == null) afk = AfkConfig.defaults();
        if (feedback == null) feedback = FeedbackConfig.defaults();
    }

    /** Compatibility constructor retained for existing tests/integrations created before feedback settings existed. */
    public UtilityConfig(
            Set<Feature> enabledFeatures,
            int feedFoodLevel,
            float feedSaturation,
            boolean feedResetExhaustion,
            long feedCooldownNanos,
            boolean healClearFire,
            boolean healClearNegativeEffects,
            Set<String> healNegativeEffects,
            long healCooldownNanos) {
        this(
                enabledFeatures,
                feedFoodLevel,
                feedSaturation,
                feedResetExhaustion,
                feedCooldownNanos,
                healClearFire,
                healClearNegativeEffects,
                healNegativeEffects,
                healCooldownNanos,
                AfkConfig.defaults(),
                FeedbackConfig.defaults());
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

        AfkConfig afk = new AfkConfig(
                readBoolean(config, "afk.auto-timeout.enabled", true),
                secondsToNanos(readLong(config, "afk.auto-timeout.seconds", 300L, 10L, 86_400L)),
                secondsToTicks(readLong(config, "afk.auto-timeout.scan-interval-seconds", 10L, 1L, 300L)),
                readBoolean(config, "afk.announcements.enabled", true),
                readBoolean(config, "afk.reset-on.movement", true),
                readBoolean(config, "afk.reset-on.chat", true),
                readBoolean(config, "afk.reset-on.command", true),
                readBoolean(config, "afk.reset-on.interaction", true),
                readBoolean(config, "afk.reset-on.block-change", true),
                readBoolean(config, "afk.reset-on.damage", true),
                readString(config, "afk.placeholder.active", "", 128),
                readString(config, "afk.placeholder.afk", " <gray>[AFK]</gray>", 128));

        FeedbackConfig feedback = new FeedbackConfig(
                readBoolean(config, "feedback.utility-success-actionbar", true),
                readBoolean(config, "feedback.social-events.prefix", false),
                readBoolean(config, "feedback.afk.bossbar.enabled", true),
                readEnum(config, "feedback.afk.bossbar.color", "YELLOW", BOSSBAR_COLORS),
                readEnum(config, "feedback.afk.bossbar.overlay", "PROGRESS", BOSSBAR_OVERLAYS),
                readBoolean(config, "feedback.afk.return-actionbar.enabled", true),
                secondsToNanos(readLong(config, "feedback.afk.suppress-short-return-seconds", 8L, 0L, 300L)));

        return new UtilityConfig(
                enabled,
                foodLevel,
                (float) configuredSaturation,
                resetExhaustion,
                feedCooldown,
                clearFire,
                clearNegative,
                effects,
                healCooldown,
                afk,
                feedback);
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

    private static String readString(FileConfiguration config, String path, String fallback, int maxLength) {
        Object raw = config.get(path);
        if (raw == null) return fallback;
        if (!(raw instanceof String value)) throw invalid(path, "must be a string");
        if (value.length() > maxLength || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw invalid(path, "must be a single-line string up to " + maxLength + " characters");
        }
        return value;
    }

    private static String readEnum(FileConfiguration config, String path, String fallback, Set<String> allowed) {
        Object raw = config.get(path);
        if (raw == null) return fallback;
        if (!(raw instanceof String value)) throw invalid(path, "must be a string");
        return normalizeEnum(path, value, allowed);
    }

    private static String normalizeEnum(String path, String value, Set<String> allowed) {
        if (value == null) throw invalid(path, "must not be null");
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!allowed.contains(normalized)) throw invalid(path, "must be one of " + allowed);
        return normalized;
    }

    private static String validatePlaceholder(String path, String value) {
        if (value == null) throw invalid(path, "must not be null");
        if (value.length() > 128 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw invalid(path, "must be a single-line string up to 128 characters");
        }
        return value;
    }

    private static long secondsToNanos(long seconds) {
        return Math.multiplyExact(seconds, 1_000_000_000L);
    }

    private static long secondsToTicks(long seconds) {
        return Math.multiplyExact(seconds, 20L);
    }

    private static IllegalArgumentException invalid(String path, String detail) {
        return new IllegalArgumentException(path + " " + detail);
    }
}
