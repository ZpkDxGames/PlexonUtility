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
        FeedbackConfig feedback,
        AdminConfig admin) {

    private static final Set<String> BOSSBAR_COLORS = Set.of("PINK", "BLUE", "RED", "GREEN", "YELLOW", "PURPLE", "WHITE");
    private static final Set<String> BOSSBAR_OVERLAYS = Set.of("PROGRESS", "NOTCHED_6", "NOTCHED_10", "NOTCHED_12", "NOTCHED_20");
    private static final Set<String> SYNTHETIC_AUDIENCES = Set.of("ORDINARY_PLAYERS", "ALL_EXCEPT_SELF");

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
            return new AfkConfig(true, secondsToNanos(300L), secondsToTicks(10L), true,
                    true, true, true, true, true, true, "", " <gray>[AFK]</gray>");
        }
    }

    /** Presentation policy for routine player feedback. */
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

    public record SyntheticPresenceConfig(
            boolean enabled,
            boolean preferPlexonChats,
            String audience,
            String quitTemplate,
            String joinTemplate) {

        public SyntheticPresenceConfig {
            audience = normalizeEnum("admin.vanish.synthetic-presence.audience", audience, SYNTHETIC_AUDIENCES);
            quitTemplate = validateTemplate("admin.vanish.synthetic-presence.fallback.quit", quitTemplate);
            joinTemplate = validateTemplate("admin.vanish.synthetic-presence.fallback.join", joinTemplate);
        }

        public static SyntheticPresenceConfig defaults() {
            return new SyntheticPresenceConfig(
                    true,
                    true,
                    "ORDINARY_PLAYERS",
                    "<gray><player> left the game</gray>",
                    "<gray><player> joined the game</gray>");
        }
    }

    public record VanishConfig(
            boolean enabled,
            boolean persist,
            boolean suppressJoinQuit,
            SyntheticPresenceConfig syntheticPresence) {

        public VanishConfig {
            if (syntheticPresence == null) syntheticPresence = SyntheticPresenceConfig.defaults();
        }

        /** Source-compatibility constructor for the 3.3/3.4 record shape. */
        public VanishConfig(boolean enabled, boolean persist, boolean suppressJoinQuit) {
            this(enabled, persist, suppressJoinQuit, SyntheticPresenceConfig.defaults());
        }

        public static VanishConfig defaults() {
            return new VanishConfig(true, true, true, SyntheticPresenceConfig.defaults());
        }
    }

    public record ModerationConfig(
            boolean kickEnabled,
            String kickDefaultReason,
            boolean banEnabled,
            String banDefaultReason,
            int maxDurationDays) {

        public ModerationConfig {
            kickDefaultReason = validateAdminReason("admin.moderation.kick.default-reason", kickDefaultReason);
            banDefaultReason = validateAdminReason("admin.moderation.ban.default-reason", banDefaultReason);
            if (maxDurationDays < 1 || maxDurationDays > 36_500) {
                throw new IllegalArgumentException("admin.moderation.ban.max-duration-days must be from 1 to 36500");
            }
        }

        public static ModerationConfig defaults() {
            return new ModerationConfig(true, "Removed by staff.", true, "Banned by staff.", 3_650);
        }
    }

    public record KillAllConfig(
            boolean enabled,
            int maxRadius,
            int confirmationThreshold,
            boolean protectNamed,
            boolean protectTamed,
            boolean protectVillagers,
            boolean protectArmorStands,
            boolean protectDisplays,
            boolean protectPluginMetadata) {

        public KillAllConfig {
            if (maxRadius < 1 || maxRadius > 4_096) {
                throw new IllegalArgumentException("admin.entity-management.killall.max-radius must be from 1 to 4096");
            }
            if (confirmationThreshold < 1 || confirmationThreshold > 100_000) {
                throw new IllegalArgumentException("admin.entity-management.killall.confirmation-threshold must be from 1 to 100000");
            }
        }

        public static KillAllConfig defaults() {
            return new KillAllConfig(true, 512, 250, true, true, true, true, true, true);
        }
    }

    public record SpawnMobConfig(
            boolean enabled,
            int maxAmount,
            boolean safeLocationSearch,
            Set<String> blockedEntityTypes) {

        public SpawnMobConfig {
            if (maxAmount < 1 || maxAmount > 100) {
                throw new IllegalArgumentException("admin.entity-management.spawnmob.max-amount must be from 1 to 100");
            }
            blockedEntityTypes = Collections.unmodifiableSet(new LinkedHashSet<>(
                    blockedEntityTypes == null ? Set.of() : blockedEntityTypes));
        }

        public static SpawnMobConfig defaults() {
            return new SpawnMobConfig(true, 100, true, Set.of());
        }
    }

    public record EntityManagementConfig(
            boolean enabled,
            KillAllConfig killall,
            SpawnMobConfig spawnmob) {

        public EntityManagementConfig {
            if (killall == null) killall = KillAllConfig.defaults();
            if (spawnmob == null) spawnmob = SpawnMobConfig.defaults();
        }

        public static EntityManagementConfig defaults() {
            return new EntityManagementConfig(true, KillAllConfig.defaults(), SpawnMobConfig.defaults());
        }
    }

    public record PlayerManagementConfig(
            boolean gamemodeEnabled,
            boolean flyEnabled,
            boolean godEnabled,
            boolean godPersist,
            boolean speedEnabled,
            boolean clearInventoryEnabled,
            boolean anvilEnabled) {

        public static PlayerManagementConfig defaults() {
            return new PlayerManagementConfig(true, true, true, false, true, true, true);
        }
    }

    public record AdminConfig(
            boolean enabled,
            VanishConfig vanish,
            ModerationConfig moderation,
            boolean prisonEnabled,
            boolean inventoryInspectionEnabled,
            EntityManagementConfig entityManagement,
            PlayerManagementConfig playerManagement) {

        public AdminConfig {
            if (vanish == null) vanish = VanishConfig.defaults();
            if (moderation == null) moderation = ModerationConfig.defaults();
            if (entityManagement == null) entityManagement = EntityManagementConfig.defaults();
            if (playerManagement == null) playerManagement = PlayerManagementConfig.defaults();
        }

        /** Source-compatibility constructor for the 3.3/3.4 record shape. */
        public AdminConfig(boolean enabled, VanishConfig vanish, ModerationConfig moderation,
                           boolean prisonEnabled, boolean inventoryInspectionEnabled) {
            this(enabled, vanish, moderation, prisonEnabled, inventoryInspectionEnabled,
                    EntityManagementConfig.defaults(), PlayerManagementConfig.defaults());
        }

        public static AdminConfig defaults() {
            return new AdminConfig(true, VanishConfig.defaults(), ModerationConfig.defaults(), true, true,
                    EntityManagementConfig.defaults(), PlayerManagementConfig.defaults());
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
        if (admin == null) admin = AdminConfig.defaults();
    }

    /** Compatibility constructor retained for source compiled against the pre-3.3 record shape. */
    public UtilityConfig(
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
        this(enabledFeatures, feedFoodLevel, feedSaturation, feedResetExhaustion, feedCooldownNanos,
                healClearFire, healClearNegativeEffects, healNegativeEffects, healCooldownNanos,
                afk, feedback, AdminConfig.defaults());
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
        this(enabledFeatures, feedFoodLevel, feedSaturation, feedResetExhaustion, feedCooldownNanos,
                healClearFire, healClearNegativeEffects, healNegativeEffects, healCooldownNanos,
                AfkConfig.defaults(), FeedbackConfig.defaults(), AdminConfig.defaults());
    }

    public static UtilityConfig from(FileConfiguration config) {
        EnumSet<Feature> enabled = EnumSet.noneOf(Feature.class);
        for (Feature feature : Feature.values()) {
            if (readBoolean(config, "features." + feature.id(), true)) enabled.add(feature);
        }

        int foodLevel = readInt(config, "feed.food-level", 20, 0, 20);
        double configuredSaturation = readDouble(config, "feed.saturation", 20.0D, 0.0D, 20.0D);
        if (configuredSaturation > foodLevel) throw invalid("feed.saturation", "must not exceed feed.food-level");
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

        SyntheticPresenceConfig syntheticPresence = new SyntheticPresenceConfig(
                readBoolean(config, "admin.vanish.synthetic-presence.enabled", true),
                readBoolean(config, "admin.vanish.synthetic-presence.prefer-plexonchats", true),
                readEnum(config, "admin.vanish.synthetic-presence.audience", "ORDINARY_PLAYERS", SYNTHETIC_AUDIENCES),
                readString(config, "admin.vanish.synthetic-presence.fallback.quit", "<gray><player> left the game</gray>", 512),
                readString(config, "admin.vanish.synthetic-presence.fallback.join", "<gray><player> joined the game</gray>", 512));
        VanishConfig vanish = new VanishConfig(
                readBoolean(config, "admin.vanish.enabled", true),
                readBoolean(config, "admin.vanish.persist", true),
                readBoolean(config, "admin.vanish.suppress-join-quit", true),
                syntheticPresence);

        ModerationConfig moderation = new ModerationConfig(
                readBoolean(config, "admin.moderation.kick.enabled", true),
                readString(config, "admin.moderation.kick.default-reason", "Removed by staff.", 160),
                readBoolean(config, "admin.moderation.ban.enabled", true),
                readString(config, "admin.moderation.ban.default-reason", "Banned by staff.", 160),
                readInt(config, "admin.moderation.ban.max-duration-days", 3_650, 1, 36_500));

        KillAllConfig killall = new KillAllConfig(
                readBoolean(config, "admin.entity-management.killall.enabled", true),
                readInt(config, "admin.entity-management.killall.max-radius", 512, 1, 4_096),
                readInt(config, "admin.entity-management.killall.confirmation-threshold", 250, 1, 100_000),
                readBoolean(config, "admin.entity-management.killall.protect.named", true),
                readBoolean(config, "admin.entity-management.killall.protect.tamed", true),
                readBoolean(config, "admin.entity-management.killall.protect.villagers", true),
                readBoolean(config, "admin.entity-management.killall.protect.armor-stands", true),
                readBoolean(config, "admin.entity-management.killall.protect.displays", true),
                readBoolean(config, "admin.entity-management.killall.protect.plugin-metadata", true));
        SpawnMobConfig spawnmob = new SpawnMobConfig(
                readBoolean(config, "admin.entity-management.spawnmob.enabled", true),
                readInt(config, "admin.entity-management.spawnmob.max-amount", 100, 1, 100),
                readBoolean(config, "admin.entity-management.spawnmob.safe-location-search", true),
                readStringSet(config, "admin.entity-management.spawnmob.blocked-entity-types"));
        EntityManagementConfig entityManagement = new EntityManagementConfig(
                readBoolean(config, "admin.entity-management.enabled", true), killall, spawnmob);

        PlayerManagementConfig playerManagement = new PlayerManagementConfig(
                readBoolean(config, "admin.player-management.gamemode.enabled", true),
                readBoolean(config, "admin.player-management.fly.enabled", true),
                readBoolean(config, "admin.player-management.god.enabled", true),
                readBoolean(config, "admin.player-management.god.persist", false),
                readBoolean(config, "admin.player-management.speed.enabled", true),
                readBoolean(config, "admin.player-management.clearinventory.enabled", true),
                readBoolean(config, "admin.player-management.anvil.enabled", true));

        AdminConfig admin = new AdminConfig(
                readBoolean(config, "admin.enabled", true),
                vanish,
                moderation,
                readBoolean(config, "admin.prison.enabled", true),
                readBoolean(config, "admin.inventory-inspection.enabled", true),
                entityManagement,
                playerManagement);

        return new UtilityConfig(enabled, foodLevel, (float) configuredSaturation, resetExhaustion,
                feedCooldown, clearFire, clearNegative, effects, healCooldown, afk, feedback, admin);
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
        if (!Double.isFinite(value) || value < min || value > max) throw invalid(path, "must be from " + min + " to " + max);
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
            values.add(text.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
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

    private static String validateTemplate(String path, String value) {
        if (value == null) throw invalid(path, "must not be null");
        if (value.length() > 512 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw invalid(path, "must be a single-line string up to 512 characters");
        }
        return value;
    }

    private static String validateAdminReason(String path, String value) {
        if (value == null || value.isBlank()) throw invalid(path, "must not be blank");
        if (value.length() > 160 || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0) {
            throw invalid(path, "must be a single-line string up to 160 characters");
        }
        return value.trim();
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
