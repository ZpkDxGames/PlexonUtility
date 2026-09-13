package com.zpkdxgames.plexonutility.command;

import com.zpkdxgames.plexonutility.admin.player.PlayerManagementService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** Permission-separated command layer for common player administration. */
public final class PlayerAdminCommand implements TabExecutor {
    private final Supplier<UtilityConfig> config;
    private final MessageService messages;
    private final PlayerManagementService players;

    public PlayerAdminCommand(Supplier<UtilityConfig> config, MessageService messages, PlayerManagementService players) {
        this.config = Objects.requireNonNull(config, "config");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.players = Objects.requireNonNull(players, "players");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "gamemode" -> gameMode(sender, args);
            case "fly" -> fly(sender, args);
            case "god" -> god(sender, args);
            case "speed" -> speed(sender, args);
            case "clearinventory" -> clearInventory(sender, args);
            case "anvil" -> anvil(sender, args);
            default -> false;
        };
    }

    private boolean gameMode(CommandSender sender, String[] args) {
        if (!feature(sender, config.get().admin().playerManagement().gamemodeEnabled(), "plexonutility.admin.gamemode")) return true;
        if (args.length < 1 || args.length > 2) return false;
        GameMode mode = parseMode(args[0]);
        if (mode == null) {
            messages.send(sender, "admin-gamemode-invalid", Map.of("mode", args[0]));
            return true;
        }
        Player target = target(sender, args.length == 2 ? args[1] : null, "plexonutility.admin.gamemode.others");
        if (target == null) return true;
        players.setGameMode(sender, target, mode);
        messages.send(sender, "admin-gamemode-success", Map.of("player", target.getName(), "mode", mode.name().toLowerCase(Locale.ROOT)));
        return true;
    }

    private boolean fly(CommandSender sender, String[] args) {
        if (!feature(sender, config.get().admin().playerManagement().flyEnabled(), "plexonutility.admin.fly")) return true;
        if (args.length > 2) return false;
        Boolean requested = null;
        String targetName = null;
        if (args.length >= 1) {
            requested = state(args[0]);
            if (requested == null) targetName = args[0];
        }
        if (args.length == 2) {
            if (requested == null) return false;
            targetName = args[1];
        }
        Player target = target(sender, targetName, "plexonutility.admin.fly.others");
        if (target == null) return true;
        if (!players.canManageFlight(target)) {
            messages.send(sender, "admin-fly-gamemode-managed", Map.of("player", target.getName()));
            return true;
        }
        boolean enabled = requested == null ? players.toggleFlight(sender, target) : players.setFlight(sender, target, requested);
        messages.send(sender, enabled ? "admin-fly-on" : "admin-fly-off", Map.of("player", target.getName()));
        return true;
    }

    private boolean god(CommandSender sender, String[] args) {
        if (!feature(sender, config.get().admin().playerManagement().godEnabled(), "plexonutility.admin.god")) return true;
        if (args.length > 2) return false;
        Boolean requested = null;
        String targetName = null;
        if (args.length >= 1) {
            requested = state(args[0]);
            if (requested == null) targetName = args[0];
        }
        if (args.length == 2) {
            if (requested == null) return false;
            targetName = args[1];
        }
        Player target = target(sender, targetName, "plexonutility.admin.god.others");
        if (target == null) return true;
        boolean enabled = requested == null ? players.toggleGodMode(sender, target) : players.setGodMode(sender, target, requested);
        messages.send(sender, enabled ? "admin-god-on" : "admin-god-off", Map.of("player", target.getName()));
        return true;
    }

    private boolean speed(CommandSender sender, String[] args) {
        if (!feature(sender, config.get().admin().playerManagement().speedEnabled(), "plexonutility.admin.speed")) return true;
        if (args.length < 1 || args.length > 3) return false;

        PlayerManagementService.SpeedMode mode = null;
        String targetName = null;
        if (args.length >= 2) {
            mode = parseSpeedMode(args[1]);
            if (mode == null) targetName = args[1];
        }
        if (args.length == 3) {
            if (mode == null) return false;
            targetName = args[2];
        }
        Player target = target(sender, targetName, "plexonutility.admin.speed.others");
        if (target == null) return true;
        if (mode == null) mode = target.isFlying() ? PlayerManagementService.SpeedMode.FLY : PlayerManagementService.SpeedMode.WALK;

        if (args[0].equalsIgnoreCase("reset")) {
            players.resetSpeed(sender, target, mode);
            messages.send(sender, "admin-speed-reset", Map.of(
                    "player", target.getName(), "mode", mode.name().toLowerCase(Locale.ROOT)));
            return true;
        }
        int level;
        try {
            level = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            messages.send(sender, "admin-speed-invalid", Map.of("speed", args[0]));
            return true;
        }
        if (level < 1 || level > 10) {
            messages.send(sender, "admin-speed-invalid", Map.of("speed", args[0]));
            return true;
        }
        players.setSpeed(sender, target, mode, level);
        messages.send(sender, "admin-speed-success", Map.of(
                "player", target.getName(), "mode", mode.name().toLowerCase(Locale.ROOT), "speed", Integer.toString(level)));
        return true;
    }

    private boolean clearInventory(CommandSender sender, String[] args) {
        if (!feature(sender, config.get().admin().playerManagement().clearInventoryEnabled(), "plexonutility.admin.clearinventory")) return true;
        if (args.length > 1) return false;
        Player target = target(sender, args.length == 1 ? args[0] : null, "plexonutility.admin.clearinventory.others");
        if (target == null) return true;
        int stacks = players.clearInventory(sender, target);
        messages.send(sender, "admin-clearinventory-success", Map.of(
                "player", target.getName(), "count", Integer.toString(stacks)));
        return true;
    }

    private boolean anvil(CommandSender sender, String[] args) {
        if (!feature(sender, config.get().admin().playerManagement().anvilEnabled(), "plexonutility.anvil")) return true;
        if (args.length != 0) return false;
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        player.openAnvil(null, true);
        return true;
    }

    private @Nullable Player target(CommandSender sender, @Nullable String name, String othersPermission) {
        if (name == null) {
            if (sender instanceof Player player) return player;
            messages.send(sender, "players-only");
            return null;
        }
        Player target = Bukkit.getPlayerExact(name);
        if (target == null || !target.isOnline() || !target.isConnected()) {
            messages.send(sender, "admin-target-offline");
            return null;
        }
        if (sender instanceof Player player && player.getUniqueId().equals(target.getUniqueId())) return target;
        if (sender.hasPermission(othersPermission)) return target;
        messages.send(sender, "no-permission");
        return null;
    }

    private boolean feature(CommandSender sender, boolean enabled, String permission) {
        if (!config.get().admin().enabled() || !enabled) {
            messages.send(sender, "admin-feature-disabled");
            return false;
        }
        if (sender.hasPermission(permission)) return true;
        messages.send(sender, "no-permission");
        return false;
    }

    private static @Nullable Boolean state(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "on", "true", "enable", "enabled" -> Boolean.TRUE;
            case "off", "false", "disable", "disabled" -> Boolean.FALSE;
            default -> null;
        };
    }

    private static @Nullable GameMode parseMode(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "survival", "s", "0" -> GameMode.SURVIVAL;
            case "creative", "c", "1" -> GameMode.CREATIVE;
            case "adventure", "a", "2" -> GameMode.ADVENTURE;
            case "spectator", "sp", "3" -> GameMode.SPECTATOR;
            default -> null;
        };
    }

    private static @Nullable PlayerManagementService.SpeedMode parseSpeedMode(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "walk", "walking" -> PlayerManagementService.SpeedMode.WALK;
            case "fly", "flying" -> PlayerManagementService.SpeedMode.FLY;
            default -> null;
        };
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if (name.equals("gamemode")) {
            if (args.length == 1) return filter(List.of("survival", "creative", "adventure", "spectator"), args[0]);
            if (args.length == 2 && sender.hasPermission("plexonutility.admin.gamemode.others")) return players(args[1]);
        }
        if (name.equals("fly") || name.equals("god")) {
            String others = name.equals("fly") ? "plexonutility.admin.fly.others" : "plexonutility.admin.god.others";
            if (args.length == 1) {
                List<String> choices = new ArrayList<>(List.of("on", "off"));
                if (sender.hasPermission(others)) choices.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                return filter(choices, args[0]);
            }
            if (args.length == 2 && sender.hasPermission(others)) return players(args[1]);
        }
        if (name.equals("speed")) {
            if (args.length == 1) return filter(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "reset"), args[0]);
            if (args.length == 2) {
                List<String> choices = new ArrayList<>(List.of("walk", "fly"));
                if (sender.hasPermission("plexonutility.admin.speed.others")) choices.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
                return filter(choices, args[1]);
            }
            if (args.length == 3 && parseSpeedMode(args[1]) != null && sender.hasPermission("plexonutility.admin.speed.others")) return players(args[2]);
        }
        if (name.equals("clearinventory") && args.length == 1 && sender.hasPermission("plexonutility.admin.clearinventory.others")) {
            return players(args[0]);
        }
        return List.of();
    }

    private static List<String> players(String prefix) {
        return filter(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), prefix);
    }

    private static List<String> filter(Iterable<String> source, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : source) if (value.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(value);
        return result;
    }
}
