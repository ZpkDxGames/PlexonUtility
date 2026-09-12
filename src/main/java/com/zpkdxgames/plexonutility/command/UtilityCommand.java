package com.zpkdxgames.plexonutility.command;

import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.cooldown.CooldownService;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.feedback.FeedbackService;
import com.zpkdxgames.plexonutility.message.MessageService;
import com.zpkdxgames.plexonutility.service.PlayerUtilityService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;

public final class UtilityCommand implements CommandExecutor {
    private final Supplier<UtilityConfig> config;
    private final MessageService messages;
    private final PlayerUtilityService utilities;
    private final Function<String, Player> playerLookup;

    /** Compatibility constructor; production wiring should supply FeedbackService. */
    public UtilityCommand(Supplier<UtilityConfig> config, CooldownService cooldowns, MessageService messages) {
        this(config, cooldowns, messages, null, Bukkit::getPlayerExact, null);
    }

    public UtilityCommand(Supplier<UtilityConfig> config, CooldownService cooldowns, MessageService messages,
                          FeedbackService feedback) {
        this(config, cooldowns, messages, feedback, Bukkit::getPlayerExact, null);
    }

    public UtilityCommand(Supplier<UtilityConfig> config, MessageService messages, PlayerUtilityService utilities) {
        this.config = config;
        this.messages = messages;
        this.utilities = utilities;
        this.playerLookup = Bukkit::getPlayerExact;
    }

    UtilityCommand(
            Supplier<UtilityConfig> config,
            CooldownService cooldowns,
            MessageService messages,
            Function<String, Player> playerLookup,
            ToDoubleFunction<Player> maxHealthLookup) {
        this(config, cooldowns, messages, null, playerLookup, maxHealthLookup);
    }

    UtilityCommand(
            Supplier<UtilityConfig> config,
            CooldownService cooldowns,
            MessageService messages,
            FeedbackService feedback,
            Function<String, Player> playerLookup,
            ToDoubleFunction<Player> maxHealthLookup) {
        this.config = config;
        this.messages = messages;
        this.playerLookup = playerLookup;
        this.utilities = maxHealthLookup == null
                ? new PlayerUtilityService(config, cooldowns, messages, feedback)
                : new PlayerUtilityService(config, cooldowns, messages, feedback, maxHealthLookup);
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
        if (!ensureEnabled(sender, Feature.FEED)) return true;
        Player target = resolveTarget(sender, args, "plexonutility.feed.others");
        if (target == null) return true;
        utilities.feed(sender, target, true);
        return true;
    }

    private boolean handleHeal(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (!ensureEnabled(sender, Feature.HEAL)) return true;
        Player target = resolveTarget(sender, args, "plexonutility.heal.others");
        if (target == null) return true;
        utilities.heal(sender, target, true);
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
        utilities.openEnderChest(viewer, target);
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
}
