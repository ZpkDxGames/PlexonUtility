package com.zpkdxgames.plexonutility.command;

import com.zpkdxgames.plexonutility.admin.inventory.InventoryInspectionService;
import com.zpkdxgames.plexonutility.admin.moderation.DurationParser;
import com.zpkdxgames.plexonutility.admin.moderation.ModerationService;
import com.zpkdxgames.plexonutility.admin.prison.PrisonLocation;
import com.zpkdxgames.plexonutility.admin.prison.PrisonService;
import com.zpkdxgames.plexonutility.admin.vanish.VanishService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Command parsing/authorization for the small native admin toolkit. */
public final class AdminActionCommand implements CommandExecutor {
    private final Supplier<UtilityConfig> config;
    private final MessageService messages;
    private final InventoryInspectionService inventory;
    private final VanishService vanish;
    private final ModerationService moderation;
    private final PrisonService prison;

    public AdminActionCommand(Supplier<UtilityConfig> config, MessageService messages,
                              InventoryInspectionService inventory, VanishService vanish,
                              ModerationService moderation, PrisonService prison) {
        this.config = config;
        this.messages = messages;
        this.inventory = inventory;
        this.vanish = vanish;
        this.moderation = moderation;
        this.prison = prison;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        return switch (command.getName().toLowerCase()) {
            case "invsee" -> invsee(sender, args);
            case "vanish" -> vanish(sender, args);
            case "kick" -> kick(sender, args);
            case "ban" -> ban(sender, args);
            case "unban" -> unban(sender, args);
            case "prison" -> prison(sender, args);
            default -> false;
        };
    }

    private boolean invsee(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        if (!enabled(sender, config.get().admin().inventoryInspectionEnabled(), "plexonutility.admin.invsee")) return true;
        if (!(sender instanceof Player viewer)) {
            messages.send(sender, "players-only");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline() || !target.isConnected()) {
            messages.send(sender, "admin-target-offline");
            return true;
        }
        inventory.open(viewer, target);
        return true;
    }

    private boolean vanish(CommandSender sender, String[] args) {
        if (args.length > 1) return false;
        if (!enabled(sender, config.get().admin().vanish().enabled(), "plexonutility.admin.vanish")) return true;
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        boolean state;
        if (args.length == 0) state = vanish.toggle(player, sender);
        else if (args[0].equalsIgnoreCase("on")) state = vanish.setVanished(player, true, sender);
        else if (args[0].equalsIgnoreCase("off")) state = vanish.setVanished(player, false, sender);
        else return false;
        player.sendActionBar(messages.renderUnprefixed(state ? "admin-vanish-on" : "admin-vanish-off", Map.of()));
        return true;
    }

    private boolean kick(CommandSender sender, String[] args) {
        if (args.length < 1) return false;
        if (!enabled(sender, config.get().admin().moderation().kickEnabled(), "plexonutility.admin.kick")) return true;
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline() || !target.isConnected()) {
            messages.send(sender, "admin-target-offline");
            return true;
        }
        String reason = join(args, 1);
        try {
            ModerationService.ActionResult result = moderation.kick(sender, target, reason,
                    config.get().admin().moderation().kickDefaultReason());
            if (result == ModerationService.ActionResult.TARGET_OFFLINE) messages.send(sender, "admin-target-offline");
            else messages.send(sender, "admin-kick-success", Map.of("player", target.getName()));
        } catch (IllegalArgumentException exception) {
            messages.send(sender, "admin-invalid-reason", Map.of("reason", exception.getMessage()));
        }
        return true;
    }

    private boolean ban(CommandSender sender, String[] args) {
        if (args.length < 1) return false;
        if (!enabled(sender, config.get().admin().moderation().banEnabled(), "plexonutility.admin.ban")) return true;
        Optional<OfflinePlayer> resolved = moderation.resolveKnown(args[0]);
        if (resolved.isEmpty()) {
            messages.send(sender, "admin-player-unknown");
            return true;
        }

        DurationParser.ParsedDuration duration;
        int reasonStart;
        try {
            if (args.length >= 2 && DurationParser.looksLikeDurationToken(args[1])) {
                duration = DurationParser.parse(args[1], config.get().admin().moderation().maxDurationDays());
                reasonStart = 2;
            } else {
                duration = DurationParser.parse("perm", config.get().admin().moderation().maxDurationDays());
                reasonStart = 1;
            }
        } catch (IllegalArgumentException exception) {
            messages.send(sender, "admin-invalid-duration", Map.of("reason", exception.getMessage()));
            return true;
        }

        String reason = join(args, reasonStart);
        try {
            OfflinePlayer target = resolved.get();
            moderation.ban(sender, target, duration, reason, config.get().admin().moderation().banDefaultReason());
            messages.send(sender, "admin-ban-success", Map.of(
                    "player", display(target),
                    "duration", duration.display()));
        } catch (IllegalArgumentException exception) {
            messages.send(sender, "admin-invalid-reason", Map.of("reason", exception.getMessage()));
        }
        return true;
    }

    private boolean unban(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        if (!enabled(sender, config.get().admin().moderation().banEnabled(), "plexonutility.admin.unban")) return true;
        Optional<OfflinePlayer> resolved = moderation.resolveKnown(args[0]);
        if (resolved.isEmpty()) {
            messages.send(sender, "admin-player-unknown");
            return true;
        }
        OfflinePlayer target = resolved.get();
        ModerationService.ActionResult result = moderation.unban(sender, target);
        if (result == ModerationService.ActionResult.NOT_BANNED) messages.send(sender, "admin-not-banned");
        else messages.send(sender, "admin-unban-success", Map.of("player", display(target)));
        return true;
    }

    private boolean prison(CommandSender sender, String[] args) {
        if (args.length < 1 || args.length > 2) return false;
        if (!enabled(sender, config.get().admin().prisonEnabled(), "plexonutility.admin.prison")) return true;
        return switch (args[0].toLowerCase()) {
            case "status" -> prisonStatus(sender, args);
            case "set" -> prisonSet(sender, args);
            case "goto" -> prisonGoto(sender, args);
            case "send" -> prisonSend(sender, args);
            case "clear" -> prisonClear(sender, args);
            default -> false;
        };
    }

    private boolean prisonStatus(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        Optional<PrisonLocation> current = prison.location();
        if (current.isEmpty()) {
            messages.send(sender, "admin-prison-not-configured");
            return true;
        }
        PrisonLocation location = current.get();
        messages.sendRaw(sender,
                "<gray>Prison:</gray> <green>CONFIGURED</green> <dark_gray>•</dark_gray> <white><world></white> <dark_gray>•</dark_gray> <white><coordinates></white>",
                Map.of("world", location.world(), "coordinates", location.coordinates()));
        return true;
    }

    private boolean prisonSet(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        if (!permission(sender, "plexonutility.admin.prison.set")) return true;
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        PrisonLocation location = prison.set(player);
        messages.send(sender, "admin-prison-set", Map.of("world", location.world(), "coordinates", location.coordinates()));
        return true;
    }

    private boolean prisonGoto(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        if (!permission(sender, "plexonutility.admin.prison.goto")) return true;
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        sendPrisonResult(sender, prison.gotoPrison(player), null);
        return true;
    }

    private boolean prisonSend(CommandSender sender, String[] args) {
        if (args.length != 2) return false;
        if (!permission(sender, "plexonutility.admin.prison.send")) return true;
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null || !target.isOnline() || !target.isConnected()) {
            messages.send(sender, "admin-target-offline");
            return true;
        }
        sendPrisonResult(sender, prison.send(sender, target), target.getName());
        return true;
    }

    private boolean prisonClear(CommandSender sender, String[] args) {
        if (args.length != 1) return false;
        if (!permission(sender, "plexonutility.admin.prison.clear")) return true;
        if (prison.clear(sender)) messages.send(sender, "admin-prison-cleared");
        else messages.send(sender, "admin-prison-not-configured");
        return true;
    }

    private void sendPrisonResult(CommandSender sender, PrisonService.Result result, String target) {
        switch (result) {
            case SUCCESS -> {
                if (target == null) messages.send(sender, "admin-prison-goto");
                else messages.send(sender, "admin-prison-sent", Map.of("player", target));
            }
            case NOT_CONFIGURED -> messages.send(sender, "admin-prison-not-configured");
            case WORLD_MISSING -> messages.send(sender, "admin-prison-world-missing");
            case TARGET_OFFLINE -> messages.send(sender, "admin-target-offline");
            case TELEPORT_FAILED -> messages.send(sender, "admin-prison-teleport-failed");
        }
    }

    private boolean enabled(CommandSender sender, boolean featureEnabled, String permission) {
        if (!config.get().admin().enabled() || !featureEnabled) {
            messages.send(sender, "admin-feature-disabled");
            return false;
        }
        return permission(sender, permission);
    }

    private boolean permission(CommandSender sender, String node) {
        if (sender.hasPermission(node)) return true;
        messages.send(sender, "no-permission");
        return false;
    }

    private static String display(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    private static String join(String[] args, int start) {
        if (start >= args.length) return "";
        return String.join(" ", Arrays.copyOfRange(args, start, args.length));
    }
}
