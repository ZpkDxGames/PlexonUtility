package com.zpkdxgames.plexonutility.command;

import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.cooldown.CooldownService;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public final class UtilityCommand implements CommandExecutor {
    private final Supplier<UtilityConfig> config;
    private final CooldownService cooldowns;
    private final MessageService messages;
    private final Function<String, Player> playerLookup;

    public UtilityCommand(Supplier<UtilityConfig> config, CooldownService cooldowns, MessageService messages) {
        this(config, cooldowns, messages, Bukkit::getPlayerExact);
    }

    UtilityCommand(
            Supplier<UtilityConfig> config,
            CooldownService cooldowns,
            MessageService messages,
            Function<String, Player> playerLookup) {
        this.config = config;
        this.cooldowns = cooldowns;
        this.messages = messages;
        this.playerLookup = playerLookup;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "feed" -> handleFeed(sender, args);
            case "heal" -> handleHeal(sender, args);
            case "enderchest" -> handleEnderChest(sender, args);
            case "workbench" -> handleWorkbench(sender, args);
            default -> false;
        };
    }

    private boolean handleFeed(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        UtilityConfig cfg = config.get();
        if (!ensureEnabled(sender, Feature.FEED)) return true;
        Player target = resolveTarget(sender, args, "plexonutility.feed.others");
        if (target == null) return true;
        boolean self = sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId());
        if (self && !checkCooldown(sender, target, Feature.FEED, cfg.feedCooldownNanos(), "plexonutility.feed.cooldown.bypass")) return true;

        target.setFoodLevel(cfg.feedFoodLevel());
        target.setSaturation(cfg.feedSaturation());
        if (cfg.feedResetExhaustion()) target.setExhaustion(0.0F);
        if (self) cooldowns.start(target.getUniqueId(), Feature.FEED, cfg.feedCooldownNanos());
        messages.send(sender, self ? "feed-self" : "feed-other", Map.of("player", target.getName()));
        return true;
    }

    private boolean handleHeal(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        UtilityConfig cfg = config.get();
        if (!ensureEnabled(sender, Feature.HEAL)) return true;
        Player target = resolveTarget(sender, args, "plexonutility.heal.others");
        if (target == null) return true;
        boolean self = sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId());
        if (self && !checkCooldown(sender, target, Feature.HEAL, cfg.healCooldownNanos(), "plexonutility.heal.cooldown.bypass")) return true;

        if (target.isDead() || !target.isValid()) {
            messages.send(sender, "heal-unavailable", Map.of("player", target.getName()));
            return true;
        }

        AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null || !Double.isFinite(maxHealth.getValue()) || maxHealth.getValue() <= 0.0D) {
            messages.send(sender, "heal-unavailable", Map.of("player", target.getName()));
            return true;
        }

        target.setHealth(maxHealth.getValue());
        if (cfg.healClearFire()) target.setFireTicks(0);
        if (cfg.healClearNegativeEffects()) {
            for (String effectName : cfg.healNegativeEffects()) {
                PotionEffectType type = PotionEffectType.getByName(effectName);
                if (type != null) target.removePotionEffect(type);
            }
        }
        if (self) cooldowns.start(target.getUniqueId(), Feature.HEAL, cfg.healCooldownNanos());
        messages.send(sender, self ? "heal-self" : "heal-other", Map.of("player", target.getName()));
        return true;
    }

    private boolean handleEnderChest(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (!ensureEnabled(sender, Feature.ENDERCHEST)) return true;
        if (!(sender instanceof Player viewer)) {
            messages.send(sender, "players-only");
            return true;
        }
        Player target = viewer;
        if (args.length == 1) {
            if (!sender.hasPermission("plexonutility.enderchest.others")) {
                messages.send(sender, "no-permission");
                return true;
            }
            target = playerLookup.apply(args[0]);
            if (target == null) {
                messages.send(sender, "player-not-found");
                return true;
            }
        }
        viewer.openInventory(target.getEnderChest());
        if (!viewer.getUniqueId().equals(target.getUniqueId())) {
            messages.send(sender, "enderchest-other", Map.of("player", target.getName()));
        }
        return true;
    }

    private boolean handleWorkbench(CommandSender sender, String[] args) {
        if (args.length != 0) return false;
        if (!ensureEnabled(sender, Feature.WORKBENCH)) return true;
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        player.openWorkbench(null, true);
        return true;
    }

    private boolean ensureEnabled(CommandSender sender, Feature feature) {
        if (config.get().enabled(feature)) return true;
        messages.send(sender, "feature-disabled");
        return false;
    }

    private Player resolveTarget(CommandSender sender, String[] args, String othersPermission) {
        if (args.length == 1) {
            if (!sender.hasPermission(othersPermission)) {
                messages.send(sender, "no-permission");
                return null;
            }
            Player target = playerLookup.apply(args[0]);
            if (target == null) messages.send(sender, "player-not-found");
            return target;
        }
        if (sender instanceof Player player) return player;
        messages.send(sender, "players-only");
        return null;
    }

    private boolean checkCooldown(CommandSender sender, Player player, Feature feature, long durationNanos, String bypassPermission) {
        if (durationNanos <= 0L || sender.hasPermission(bypassPermission)) return true;
        long remaining = cooldowns.remainingNanos(player.getUniqueId(), feature);
        if (remaining <= 0L) return true;
        long seconds = Math.max(1L, (remaining + 999_999_999L) / 1_000_000_000L);
        messages.send(sender, "cooldown", Map.of("seconds", Long.toString(seconds)));
        return false;
    }
}
