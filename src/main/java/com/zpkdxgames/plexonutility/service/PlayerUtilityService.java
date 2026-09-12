package com.zpkdxgames.plexonutility.service;

import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.cooldown.CooldownService;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.feedback.FeedbackService;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

/** Reusable player-utility business logic shared by commands and the admin GUI. */
public final class PlayerUtilityService {
    private final Supplier<UtilityConfig> config;
    private final CooldownService cooldowns;
    private final MessageService messages;
    private final FeedbackService feedback;
    private final ToDoubleFunction<Player> maxHealthLookup;

    public PlayerUtilityService(Supplier<UtilityConfig> config, CooldownService cooldowns, MessageService messages,
                                FeedbackService feedback) {
        this(config, cooldowns, messages, feedback, PlayerUtilityService::readMaxHealth);
    }

    public PlayerUtilityService(Supplier<UtilityConfig> config, CooldownService cooldowns, MessageService messages,
                                FeedbackService feedback, ToDoubleFunction<Player> maxHealthLookup) {
        this.config = Objects.requireNonNull(config, "config");
        this.cooldowns = Objects.requireNonNull(cooldowns, "cooldowns");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.feedback = feedback;
        this.maxHealthLookup = Objects.requireNonNull(maxHealthLookup, "maxHealthLookup");
    }

    public boolean feed(CommandSender actor, Player target, boolean respectSelfCooldown) {
        UtilityConfig cfg = config.get();
        boolean self = actor instanceof Player player && player.getUniqueId().equals(target.getUniqueId());
        if (self && respectSelfCooldown
                && !checkCooldown(actor, target, Feature.FEED, cfg.feedCooldownNanos(), "plexonutility.feed.cooldown.bypass")) return false;

        target.setFoodLevel(cfg.feedFoodLevel());
        target.setSaturation(cfg.feedSaturation());
        if (cfg.feedResetExhaustion()) target.setExhaustion(0.0F);
        if (self && respectSelfCooldown) cooldowns.start(target.getUniqueId(), Feature.FEED, cfg.feedCooldownNanos());
        if (self && feedback != null) feedback.success(actor, "feed-self", Map.of("player", target.getName()));
        else messages.send(actor, self ? "feed-self" : "feed-other", Map.of("player", target.getName()));
        return true;
    }

    public boolean heal(CommandSender actor, Player target, boolean respectSelfCooldown) {
        UtilityConfig cfg = config.get();
        boolean self = actor instanceof Player player && player.getUniqueId().equals(target.getUniqueId());
        if (self && respectSelfCooldown
                && !checkCooldown(actor, target, Feature.HEAL, cfg.healCooldownNanos(), "plexonutility.heal.cooldown.bypass")) return false;
        if (target.isDead() || !target.isValid()) {
            messages.send(actor, "heal-unavailable", Map.of("player", target.getName()));
            return false;
        }

        double maxHealth = maxHealthLookup.applyAsDouble(target);
        if (!Double.isFinite(maxHealth) || maxHealth <= 0.0D) {
            messages.send(actor, "heal-unavailable", Map.of("player", target.getName()));
            return false;
        }

        target.setHealth(maxHealth);
        if (cfg.healClearFire()) target.setFireTicks(0);
        if (cfg.healClearNegativeEffects()) {
            for (String effectName : cfg.healNegativeEffects()) {
                PotionEffectType type = PotionEffectType.getByName(effectName);
                if (type != null) target.removePotionEffect(type);
            }
        }
        if (self && respectSelfCooldown) cooldowns.start(target.getUniqueId(), Feature.HEAL, cfg.healCooldownNanos());
        if (self && feedback != null) feedback.success(actor, "heal-self", Map.of("player", target.getName()));
        else messages.send(actor, self ? "heal-self" : "heal-other", Map.of("player", target.getName()));
        return true;
    }

    public void openEnderChest(Player viewer, Player target) {
        viewer.openInventory(target.getEnderChest());
        if (!viewer.getUniqueId().equals(target.getUniqueId())) {
            messages.send(viewer, "enderchest-other", Map.of("player", target.getName()));
        }
    }

    private boolean checkCooldown(CommandSender sender, Player player, Feature feature, long durationNanos, String bypassPermission) {
        if (durationNanos <= 0L || sender.hasPermission(bypassPermission)) return true;
        long remaining = cooldowns.remainingNanos(player.getUniqueId(), feature);
        if (remaining <= 0L) return true;
        long seconds = Math.max(1L, (remaining + 999_999_999L) / 1_000_000_000L);
        messages.send(sender, "cooldown", Map.of("seconds", Long.toString(seconds)));
        return false;
    }

    private static double readMaxHealth(Player player) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        return maxHealth == null ? Double.NaN : maxHealth.getValue();
    }
}
