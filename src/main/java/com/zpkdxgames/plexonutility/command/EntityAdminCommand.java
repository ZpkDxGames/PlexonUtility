package com.zpkdxgames.plexonutility.command;

import com.zpkdxgames.plexonutility.admin.entity.EntityCleanupService;
import com.zpkdxgames.plexonutility.admin.entity.EntitySelector;
import com.zpkdxgames.plexonutility.admin.entity.EntitySpawnService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Command/permission layer for bounded entity administration. */
public final class EntityAdminCommand implements TabExecutor {
    private static final long CONFIRM_TTL_NANOS = TimeUnit.SECONDS.toNanos(15);
    private static final List<String> CATEGORIES = List.of(
            "hostile", "monsters", "passive", "animals", "ambient", "aquatic", "water", "bosses", "mobs", "all");
    private static final List<String> RADIUS_SUGGESTIONS = List.of("32", "64", "128", "256", "512");
    private static final List<String> AMOUNT_SUGGESTIONS = List.of("1", "5", "10", "25", "50", "100");

    private final Supplier<UtilityConfig> config;
    private final MessageService messages;
    private final EntityCleanupService cleanup;
    private final EntitySpawnService spawning;
    private final TimedConfirmationStore<EntityCleanupService.Query> pending =
            new TimedConfirmationStore<>(CONFIRM_TTL_NANOS, System::nanoTime);

    public EntityAdminCommand(Supplier<UtilityConfig> config, MessageService messages,
                              EntityCleanupService cleanup, EntitySpawnService spawning) {
        this.config = Objects.requireNonNull(config, "config");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.cleanup = Objects.requireNonNull(cleanup, "cleanup");
        this.spawning = Objects.requireNonNull(spawning, "spawning");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        return switch (command.getName().toLowerCase(Locale.ROOT)) {
            case "killall" -> killAll(sender, args);
            case "spawnmob" -> spawnMob(sender, args);
            default -> false;
        };
    }

    private boolean killAll(CommandSender sender, String[] args) {
        UtilityConfig.AdminConfig admin = config.get().admin();
        UtilityConfig.EntityManagementConfig entity = admin.entityManagement();
        if (!enabled(sender, admin.enabled() && entity.enabled() && entity.killall().enabled(),
                "plexonutility.admin.killall")) return true;

        if (args.length == 1 && args[0].equalsIgnoreCase("confirm")) return confirm(sender);
        if (args.length < 1 || args.length > 3) return false;
        pending.clear(actorKey(sender));

        EntitySelector.Selection selection;
        try {
            selection = EntitySelector.parse(args[0]);
        } catch (IllegalArgumentException exception) {
            messages.send(sender, "admin-killall-invalid-selector", Map.of("selector", args[0]));
            return true;
        }

        EntityCleanupService.Query query = query(sender, selection, args);
        if (query == null) return true;
        EntityCleanupService.Preview preview = cleanup.preview(query);
        if (preview.removable() == 0) {
            messages.send(sender, "admin-killall-none", Map.of(
                    "matched", Integer.toString(preview.matched()),
                    "protected", Integer.toString(preview.protectedCount())));
            return true;
        }

        int threshold = entity.killall().confirmationThreshold();
        if (preview.removable() >= threshold && !sender.hasPermission("plexonutility.admin.killall.bypass-confirm")) {
            pending.put(actorKey(sender), query);
            messages.send(sender, "admin-killall-confirm", Map.of(
                    "count", Integer.toString(preview.removable()), "seconds", "15"));
            return true;
        }
        sendCleanupResult(sender, query, cleanup.execute(sender, query));
        return true;
    }

    private boolean confirm(CommandSender sender) {
        EntityCleanupService.Query query = pending.consume(actorKey(sender)).orElse(null);
        if (query == null) {
            messages.send(sender, "admin-killall-confirm-expired");
            return true;
        }
        sendCleanupResult(sender, query, cleanup.execute(sender, query));
        return true;
    }

    private @Nullable EntityCleanupService.Query query(CommandSender sender, EntitySelector.Selection selection, String[] args) {
        UtilityConfig.KillAllConfig policy = config.get().admin().entityManagement().killall();
        if (args.length == 1) {
            if (!(sender instanceof Player player)) {
                messages.send(sender, "admin-killall-console-world-required");
                return null;
            }
            return new EntityCleanupService.Query(selection, player.getWorld(), null, null);
        }

        String scope = args[1];
        if (scope.equalsIgnoreCase("world")) {
            if (!permission(sender, "plexonutility.admin.killall.world")) return null;
            World world;
            if (args.length == 3) {
                world = Bukkit.getWorld(args[2]);
                if (world == null) {
                    messages.send(sender, "admin-world-not-found", Map.of("world", args[2]));
                    return null;
                }
            } else if (sender instanceof Player player) {
                world = player.getWorld();
            } else {
                messages.send(sender, "admin-killall-console-world-required");
                return null;
            }
            return new EntityCleanupService.Query(selection, world, null, null);
        }

        if (args.length != 2 || !(sender instanceof Player player)) {
            messages.send(sender, "admin-killall-invalid-radius", Map.of("radius", scope));
            return null;
        }
        int radius;
        try {
            radius = Integer.parseInt(scope);
        } catch (NumberFormatException exception) {
            messages.send(sender, "admin-killall-invalid-radius", Map.of("radius", scope));
            return null;
        }
        if (radius < 1 || radius > policy.maxRadius()) {
            messages.send(sender, "admin-killall-invalid-radius", Map.of("radius", scope));
            return null;
        }
        return new EntityCleanupService.Query(selection, player.getWorld(), player.getLocation(), (double) radius);
    }

    private void sendCleanupResult(CommandSender sender, EntityCleanupService.Query query, EntityCleanupService.Result result) {
        messages.send(sender, "admin-killall-success", Map.of(
                "count", Integer.toString(result.removed()),
                "protected", Integer.toString(result.protectedCount()),
                "selector", query.selection().canonical(),
                "world", query.world().getName()));
    }

    private boolean spawnMob(CommandSender sender, String[] args) {
        UtilityConfig.AdminConfig admin = config.get().admin();
        UtilityConfig.EntityManagementConfig entity = admin.entityManagement();
        if (!enabled(sender, admin.enabled() && entity.enabled() && entity.spawnmob().enabled(),
                "plexonutility.admin.spawnmob")) return true;
        if (args.length < 1 || args.length > 2) return false;
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }

        EntityType type;
        try {
            type = spawning.parseType(args[0]);
        } catch (IllegalArgumentException exception) {
            messages.send(sender, "admin-spawnmob-invalid-type", Map.of("type", args[0]));
            return true;
        }
        int amount = 1;
        if (args.length == 2) {
            try {
                amount = Integer.parseInt(args[1]);
            } catch (NumberFormatException exception) {
                messages.send(sender, "admin-spawnmob-invalid-amount", Map.of("amount", args[1]));
                return true;
            }
        }
        int max = entity.spawnmob().maxAmount();
        if (amount < 1 || amount > max || amount > 100) {
            messages.send(sender, "admin-spawnmob-invalid-amount", Map.of(
                    "amount", Integer.toString(amount), "max", Integer.toString(max)));
            return true;
        }

        EntitySpawnService.SpawnResult result = spawning.spawn(sender, player, type, amount);
        if (result.location() == null) {
            messages.send(sender, "admin-spawnmob-no-safe-location");
        } else if (result.failed() > 0) {
            messages.send(sender, "admin-spawnmob-partial", Map.of(
                    "spawned", Integer.toString(result.spawned()),
                    "requested", Integer.toString(result.requested()),
                    "failed", Integer.toString(result.failed()),
                    "type", type.name().toLowerCase(Locale.ROOT)));
        } else {
            messages.send(sender, "admin-spawnmob-success", Map.of(
                    "count", Integer.toString(result.spawned()),
                    "type", type.name().toLowerCase(Locale.ROOT)));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("spawnmob")) return completeSpawn(sender, args);
        if (command.getName().equalsIgnoreCase("killall")) return completeCleanup(sender, args);
        return List.of();
    }

    private List<String> completeCleanup(CommandSender sender, String[] args) {
        if (!sender.hasPermission("plexonutility.admin.killall")) return List.of();
        if (args.length == 1) {
            Set<String> choices = new LinkedHashSet<>(CATEGORIES);
            choices.addAll(EntitySelector.livingNames());
            if (pending.hasValid(actorKey(sender))) choices.add("confirm");
            return filter(choices, args[0]);
        }
        if (args.length == 2 && sender instanceof Player) {
            List<String> choices = new ArrayList<>();
            for (String radius : RADIUS_SUGGESTIONS) {
                if (Integer.parseInt(radius) <= config.get().admin().entityManagement().killall().maxRadius()) choices.add(radius);
            }
            if (sender.hasPermission("plexonutility.admin.killall.world")) choices.add("world");
            return filter(choices, args[1]);
        }
        if (args.length == 3 && args[1].equalsIgnoreCase("world")
                && sender.hasPermission("plexonutility.admin.killall.world")) {
            return filter(Bukkit.getWorlds().stream().map(World::getName).toList(), args[2]);
        }
        return List.of();
    }

    private List<String> completeSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("plexonutility.admin.spawnmob")) return List.of();
        if (args.length == 1) return filter(spawning.allowedTypeNames(), args[0]);
        if (args.length == 2) {
            int max = config.get().admin().entityManagement().spawnmob().maxAmount();
            return filter(AMOUNT_SUGGESTIONS.stream().filter(value -> Integer.parseInt(value) <= max).toList(), args[1]);
        }
        return List.of();
    }

    private boolean enabled(CommandSender sender, boolean featureEnabled, String node) {
        if (!featureEnabled) {
            messages.send(sender, "admin-feature-disabled");
            return false;
        }
        return permission(sender, node);
    }

    private boolean permission(CommandSender sender, String node) {
        if (sender.hasPermission(node)) return true;
        messages.send(sender, "no-permission");
        return false;
    }

    private static List<String> filter(Iterable<String> source, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String value : source) if (value.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(value);
        return result;
    }

    private static String actorKey(CommandSender sender) {
        if (sender instanceof Player player) return "player:" + player.getUniqueId();
        return "sender:" + sender.getName().toLowerCase(Locale.ROOT);
    }
}
