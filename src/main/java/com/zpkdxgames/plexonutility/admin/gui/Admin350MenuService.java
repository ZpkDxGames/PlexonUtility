package com.zpkdxgames.plexonutility.admin.gui;

import com.zpkdxgames.plexoncore.gui.GuiService;
import com.zpkdxgames.plexoncore.scheduler.CoreScheduler;
import com.zpkdxgames.plexonutility.admin.entity.EntityCleanupService;
import com.zpkdxgames.plexonutility.admin.entity.EntitySelector;
import com.zpkdxgames.plexonutility.admin.entity.EntitySpawnService;
import com.zpkdxgames.plexonutility.admin.inventory.InventoryInspectionService;
import com.zpkdxgames.plexonutility.admin.player.PlayerManagementService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.menu.UtilityMenuItemFactory;
import com.zpkdxgames.plexonutility.message.MessageService;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 3.5 administration hub layered over the stable 3.3/3.4 management screens.
 * Destructive GUI actions always use a dedicated confirmation surface.
 */
public final class Admin350MenuService {
    private static final int PAGE_SIZE = 45;
    private static final ClickCallback.Options DIALOG_CALLBACK = ClickCallback.Options.builder()
            .uses(1).lifetime(Duration.ofMinutes(5)).build();

    private final Supplier<UtilityConfig> config;
    private final MessageService messages;
    private final GuiService gui;
    private final CoreScheduler scheduler;
    private final UtilityMenuItemFactory items;
    private final AdminMenuService legacy;
    private final EntityCleanupService cleanup;
    private final EntitySpawnService spawning;
    private final PlayerManagementService players;
    private final InventoryInspectionService inventory;

    public Admin350MenuService(Supplier<UtilityConfig> config, MessageService messages, GuiService gui,
                               CoreScheduler scheduler, UtilityMenuItemFactory items, AdminMenuService legacy,
                               EntityCleanupService cleanup, EntitySpawnService spawning,
                               PlayerManagementService players, InventoryInspectionService inventory) {
        this.config = config;
        this.messages = messages;
        this.gui = gui;
        this.scheduler = scheduler;
        this.items = items;
        this.legacy = legacy;
        this.cleanup = cleanup;
        this.spawning = spawning;
        this.players = players;
        this.inventory = inventory;
    }

    public void open(Player player) {
        if (!requireFeature(player, config.get().admin().enabled(), "plexonutility.admin.menu")) return;
        var builder = gui.builder("utility", "admin-center-3.5",
                        items.render("<gradient:#57E389:#22D3EE><bold>Admin Center 3.5</bold></gradient>"), 5)
                .filler(Material.BLACK_STAINED_GLASS_PANE);
        addFrame(builder, 5);
        builder.button(10, actionIcon(player, Material.PLAYER_HEAD, "<aqua><bold>Player Management</bold></aqua>",
                "Game mode, flight, god mode and speed controls.", "plexonutility.admin.menu",
                playerManagementEnabled()), click -> openPlayerSelector(click.player(), 0, SelectorContext.CONTROLS));
        builder.button(12, actionIcon(player, Material.CHEST, "<aqua><bold>Inventory Management</bold></aqua>",
                "Inspect or deliberately clear an online player's inventory.", "plexonutility.admin.menu",
                config.get().admin().inventoryInspectionEnabled() || config.get().admin().playerManagement().clearInventoryEnabled()),
                click -> openPlayerSelector(click.player(), 0, SelectorContext.INVENTORY));
        builder.button(14, actionIcon(player, Material.SPAWNER, "<gold><bold>Entity Management</bold></gold>",
                "Bounded cleanup and registry-backed mob spawning.", "plexonutility.admin.menu",
                config.get().admin().entityManagement().enabled()), click -> openEntityManagement(click.player()));
        builder.button(16, actionIcon(player, Material.COMMAND_BLOCK, "<yellow><bold>Utility Admin Tools</bold></yellow>",
                "Existing vanish, moderation, prison and integration controls.", "plexonutility.admin.menu", true),
                click -> legacy.open(click.player()));
        builder.button(22, actionIcon(player, Material.ANVIL, "<aqua><bold>Open Anvil</bold></aqua>",
                "Open the native virtual anvil for yourself.", "plexonutility.anvil",
                config.get().admin().playerManagement().anvilEnabled()), click -> openAnvil(click.player()));
        builder.button(36, items.icon(Material.ARROW, "<aqua><bold>Classic Admin Center</bold></aqua>",
                List.of("<yellow>Click</yellow> <gray>to open the established moderation toolkit.</gray>")),
                click -> legacy.open(click.player()));
        builder.button(40, refreshIcon(), click -> open(click.player()));
        builder.button(44, closeIcon(), click -> click.player().closeInventory());
        builder.open(player);
    }

    public void openEntityManagement(Player player) {
        UtilityConfig.EntityManagementConfig entity = config.get().admin().entityManagement();
        if (!requireFeature(player, config.get().admin().enabled() && entity.enabled(), "plexonutility.admin.menu")) return;
        var builder = gui.builder("utility", "entity-management-3.5",
                        items.render("<gold><bold>Entity Management</bold></gold>"), 3)
                .filler(Material.BLACK_STAINED_GLASS_PANE);
        builder.button(10, actionIcon(player, Material.IRON_SWORD, "<red><bold>Entity Cleanup</bold></red>",
                "Preview a selector and scope before a mandatory confirmation.", "plexonutility.admin.killall",
                entity.killall().enabled()), click -> openCleanupDialog(click.player()));
        builder.button(13, entityPolicyIcon(), click -> { });
        builder.button(16, actionIcon(player, Material.SPAWNER, "<green><bold>Spawn Mob</bold></green>",
                "Spawn a bounded batch at a validated safe loaded location.", "plexonutility.admin.spawnmob",
                entity.spawnmob().enabled()), click -> openSpawnDialog(click.player()));
        builder.button(18, backIcon(), click -> open(click.player()));
        builder.button(22, refreshIcon(), click -> openEntityManagement(click.player()));
        builder.button(26, closeIcon(), click -> click.player().closeInventory());
        builder.open(player);
    }

    public void openPlayerSelector(Player viewer, int requestedPage, SelectorContext context) {
        if (!require(viewer, "plexonutility.admin.menu")) return;
        List<? extends Player> online = Bukkit.getOnlinePlayers().stream()
                .filter(Player::isConnected)
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int pages = Math.max(1, (online.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        int start = page * PAGE_SIZE;
        int end = Math.min(online.size(), start + PAGE_SIZE);
        String title = context == SelectorContext.INVENTORY
                ? "<aqua><bold>Select Inventory Target</bold></aqua>"
                : "<aqua><bold>Select Player Target</bold></aqua>";
        var builder = gui.builder("utility", "admin-3.5-player-selector-" + context.name().toLowerCase(Locale.ROOT),
                        items.render(title), 6)
                .filler(Material.BLACK_STAINED_GLASS_PANE).page(page);
        for (int index = start; index < end; index++) {
            Player target = online.get(index);
            builder.button(index - start, playerIcon(viewer, target), click -> {
                Player current = online(target.getUniqueId(), click.player());
                if (current == null) {
                    openPlayerSelector(click.player(), page, context);
                    return;
                }
                if (context == SelectorContext.INVENTORY) openInventoryManagement(click.player(), current.getUniqueId());
                else openPlayerManagement(click.player(), current.getUniqueId());
            });
        }
        builder.button(45, backIcon(), click -> open(click.player()));
        if (page > 0) builder.button(47, pageIcon("Previous Page"), click -> openPlayerSelector(click.player(), page - 1, context));
        builder.button(49, pageStatusIcon(page, pages, online.size()), click -> openPlayerSelector(click.player(), page, context));
        if (page + 1 < pages) builder.button(51, pageIcon("Next Page"), click -> openPlayerSelector(click.player(), page + 1, context));
        builder.button(53, closeIcon(), click -> click.player().closeInventory());
        builder.open(viewer);
    }

    public void openPlayerManagement(Player viewer, UUID targetId) {
        Player target = online(targetId, viewer);
        if (target == null || !require(viewer, "plexonutility.admin.menu")) return;
        UtilityConfig.PlayerManagementConfig policy = config.get().admin().playerManagement();
        var builder = gui.builder("utility", "player-management-3.5",
                        items.template("<aqua><bold>Manage <player></bold></aqua>", Map.of("player", target.getName())), 5)
                .filler(Material.BLACK_STAINED_GLASS_PANE);
        addFrame(builder, 5);
        builder.button(4, playerStateIcon(target), click -> openPlayerManagement(click.player(), targetId));
        builder.button(10, actionIcon(viewer, Material.GRASS_BLOCK, "<green><bold>Game Mode</bold></green>",
                "Choose Survival, Creative, Adventure or Spectator.", "plexonutility.admin.gamemode",
                policy.gamemodeEnabled()), click -> openGameMode(click.player(), targetId));
        builder.button(12, actionIcon(viewer, Material.FEATHER, "<aqua><bold>Flight</bold></aqua>",
                players.isFlightManaged(targetId) ? "Disable Utility-managed flight." : "Enable Utility-managed flight.",
                "plexonutility.admin.fly", policy.flyEnabled()), click -> toggleFlight(click.player(), targetId));
        builder.button(14, actionIcon(viewer, Material.NETHER_STAR, "<yellow><bold>God Mode</bold></yellow>",
                players.isGodMode(targetId) ? "Disable event-driven god mode." : "Enable event-driven god mode.",
                "plexonutility.admin.god", policy.godEnabled()), click -> toggleGod(click.player(), targetId));
        builder.button(16, actionIcon(viewer, Material.SUGAR, "<gold><bold>Movement Speed</bold></gold>",
                "Apply fixed safe walk/fly speed presets or reset defaults.", "plexonutility.admin.speed",
                policy.speedEnabled()), click -> openSpeed(click.player(), targetId));
        builder.button(22, actionIcon(viewer, Material.CHEST, "<aqua><bold>Inventory Controls</bold></aqua>",
                "Inspect or clear the selected player's inventory.", "plexonutility.admin.menu",
                config.get().admin().inventoryInspectionEnabled() || policy.clearInventoryEnabled()),
                click -> openInventoryManagement(click.player(), targetId));
        builder.button(24, items.icon(Material.COMMAND_BLOCK, "<yellow><bold>Moderation / Prison</bold></yellow>",
                List.of("<gray>Open the established actions for this player.</gray>", "", "<yellow>Click</yellow> <gray>to continue.</gray>")),
                click -> legacy.openPlayerActions(click.player(), targetId));
        builder.button(36, backIcon(), click -> openPlayerSelector(click.player(), 0, SelectorContext.CONTROLS));
        builder.button(40, refreshIcon(), click -> openPlayerManagement(click.player(), targetId));
        builder.button(44, closeIcon(), click -> click.player().closeInventory());
        builder.open(viewer);
    }

    public void openInventoryManagement(Player viewer, UUID targetId) {
        Player target = online(targetId, viewer);
        if (target == null || !require(viewer, "plexonutility.admin.menu")) return;
        UtilityConfig.PlayerManagementConfig policy = config.get().admin().playerManagement();
        var builder = gui.builder("utility", "inventory-management-3.5",
                        items.template("<aqua><bold>Inventory: <player></bold></aqua>", Map.of("player", target.getName())), 3)
                .filler(Material.BLACK_STAINED_GLASS_PANE);
        builder.button(4, playerStateIcon(target), click -> openInventoryManagement(click.player(), targetId));
        builder.button(10, actionIcon(viewer, Material.CHEST, "<aqua><bold>Inspect / Edit</bold></aqua>",
                "Open the existing permission-separated live inventory view.", "plexonutility.admin.invsee",
                config.get().admin().inventoryInspectionEnabled()), click -> inspect(click.player(), targetId));
        builder.button(16, actionIcon(viewer, Material.LAVA_BUCKET, "<red><bold>Clear Inventory</bold></red>",
                "Clear storage, armor and offhand only after confirmation.", "plexonutility.admin.clearinventory",
                policy.clearInventoryEnabled()), click -> confirmClearInventory(click.player(), targetId));
        builder.button(18, backIcon(), click -> openPlayerSelector(click.player(), 0, SelectorContext.INVENTORY));
        builder.button(22, refreshIcon(), click -> openInventoryManagement(click.player(), targetId));
        builder.button(26, closeIcon(), click -> click.player().closeInventory());
        builder.open(viewer);
    }

    private void openGameMode(Player viewer, UUID targetId) {
        Player target = online(targetId, viewer);
        if (target == null || !targetPermission(viewer, target, config.get().admin().playerManagement().gamemodeEnabled(),
                "plexonutility.admin.gamemode", "plexonutility.admin.gamemode.others")) return;
        var builder = gui.builder("utility", "gamemode-3.5", items.render("<green><bold>Game Mode</bold></green>"), 3)
                .filler(Material.BLACK_STAINED_GLASS_PANE);
        builder.button(10, modeIcon(Material.IRON_PICKAXE, "Survival", target.getGameMode() == GameMode.SURVIVAL),
                click -> setGameMode(click.player(), targetId, GameMode.SURVIVAL));
        builder.button(12, modeIcon(Material.DIAMOND_BLOCK, "Creative", target.getGameMode() == GameMode.CREATIVE),
                click -> setGameMode(click.player(), targetId, GameMode.CREATIVE));
        builder.button(14, modeIcon(Material.MAP, "Adventure", target.getGameMode() == GameMode.ADVENTURE),
                click -> setGameMode(click.player(), targetId, GameMode.ADVENTURE));
        builder.button(16, modeIcon(Material.ENDER_EYE, "Spectator", target.getGameMode() == GameMode.SPECTATOR),
                click -> setGameMode(click.player(), targetId, GameMode.SPECTATOR));
        builder.button(18, backIcon(), click -> openPlayerManagement(click.player(), targetId));
        builder.button(26, closeIcon(), click -> click.player().closeInventory());
        builder.open(viewer);
    }

    private void openSpeed(Player viewer, UUID targetId) {
        Player target = online(targetId, viewer);
        if (target == null || !targetPermission(viewer, target, config.get().admin().playerManagement().speedEnabled(),
                "plexonutility.admin.speed", "plexonutility.admin.speed.others")) return;
        var builder = gui.builder("utility", "speed-3.5", items.render("<gold><bold>Movement Speed</bold></gold>"), 4)
                .filler(Material.BLACK_STAINED_GLASS_PANE);
        builder.button(10, speedIcon("Walk 1", target.getWalkSpeed(), 1), click -> setSpeed(click.player(), targetId, PlayerManagementService.SpeedMode.WALK, 1));
        builder.button(12, speedIcon("Walk 5", target.getWalkSpeed(), 5), click -> setSpeed(click.player(), targetId, PlayerManagementService.SpeedMode.WALK, 5));
        builder.button(14, speedIcon("Walk 10", target.getWalkSpeed(), 10), click -> setSpeed(click.player(), targetId, PlayerManagementService.SpeedMode.WALK, 10));
        builder.button(16, resetSpeedIcon("Reset Walk"), click -> resetSpeed(click.player(), targetId, PlayerManagementService.SpeedMode.WALK));
        builder.button(19, speedIcon("Fly 1", target.getFlySpeed(), 1), click -> setSpeed(click.player(), targetId, PlayerManagementService.SpeedMode.FLY, 1));
        builder.button(21, speedIcon("Fly 5", target.getFlySpeed(), 5), click -> setSpeed(click.player(), targetId, PlayerManagementService.SpeedMode.FLY, 5));
        builder.button(23, speedIcon("Fly 10", target.getFlySpeed(), 10), click -> setSpeed(click.player(), targetId, PlayerManagementService.SpeedMode.FLY, 10));
        builder.button(25, resetSpeedIcon("Reset Fly"), click -> resetSpeed(click.player(), targetId, PlayerManagementService.SpeedMode.FLY));
        builder.button(27, backIcon(), click -> openPlayerManagement(click.player(), targetId));
        builder.button(35, closeIcon(), click -> click.player().closeInventory());
        builder.open(viewer);
    }

    private void openCleanupDialog(Player actor) {
        UtilityConfig.EntityManagementConfig entity = config.get().admin().entityManagement();
        if (!requireFeature(actor, entity.enabled() && entity.killall().enabled(), "plexonutility.admin.killall")) return;
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(items.render("<red><bold>Preview Entity Cleanup</bold></red>"))
                        .body(List.of(DialogBody.plainMessage(items.render(
                                "<gray>Selector examples: hostile, passive, ambient, aquatic, bosses, mobs, all, zombie. Scope is a radius or world.</gray>"))))
                        .inputs(List.of(
                                DialogInput.text("selector", Component.text("Selector")).initial("hostile").maxLength(64).width(300).build(),
                                DialogInput.text("scope", Component.text("Radius or world")).initial("64").maxLength(16).width(300).build()))
                        .build())
                .type(DialogType.notice(ActionButton.create(items.render("<yellow>Preview</yellow>"), Component.empty(), 160,
                        DialogAction.customClick((view, audience) -> {
                            if (!(audience instanceof Player player)) return;
                            String selector = view.getText("selector");
                            String scope = view.getText("scope");
                            player.closeDialog();
                            scheduler.runPrimary(() -> previewCleanup(player, selector, scope));
                        }, DIALOG_CALLBACK)))));
        actor.showDialog(dialog);
    }

    private void previewCleanup(Player actor, String rawSelector, String rawScope) {
        UtilityConfig.EntityManagementConfig entity = config.get().admin().entityManagement();
        if (!requireFeature(actor, entity.enabled() && entity.killall().enabled(), "plexonutility.admin.killall")) return;
        final EntitySelector.Selection selection;
        try {
            selection = EntitySelector.parse(rawSelector == null ? "" : rawSelector.trim());
        } catch (IllegalArgumentException exception) {
            messages.send(actor, "admin-killall-invalid-selector", Map.of("selector", String.valueOf(rawSelector)));
            return;
        }
        EntityCleanupService.Query query;
        String scope = rawScope == null ? "" : rawScope.trim();
        if (scope.equalsIgnoreCase("world")) {
            if (!require(actor, "plexonutility.admin.killall.world")) return;
            query = new EntityCleanupService.Query(selection, actor.getWorld(), null, null);
        } else {
            int radius;
            try {
                radius = Integer.parseInt(scope);
            } catch (NumberFormatException exception) {
                messages.send(actor, "admin-killall-invalid-radius", Map.of("radius", scope));
                return;
            }
            if (radius < 1 || radius > entity.killall().maxRadius()) {
                messages.send(actor, "admin-killall-invalid-radius", Map.of("radius", scope));
                return;
            }
            query = new EntityCleanupService.Query(selection, actor.getWorld(), actor.getLocation(), (double) radius);
        }
        EntityCleanupService.Preview preview = cleanup.preview(query);
        if (preview.removable() == 0) {
            messages.send(actor, "admin-killall-none", Map.of(
                    "matched", Integer.toString(preview.matched()),
                    "protected", Integer.toString(preview.protectedCount())));
            return;
        }
        ItemStack subject = items.icon(Material.REDSTONE_BLOCK, "<red><bold>Confirm Entity Cleanup</bold></red>", List.of(
                "<gray>Selector:</gray> <white>" + query.selection().canonical() + "</white>",
                "<gray>Scope:</gray> <white>" + query.scopeDescription() + "</white>",
                "<gray>Removable:</gray> <red>" + preview.removable() + "</red>",
                "<gray>Protected:</gray> <green>" + preview.protectedCount() + "</green>",
                "",
                "<red>This action is destructive.</red>"));
        gui.confirmation(actor, "utility", "killall-confirm-3.5", items.render("<red><bold>Confirm Cleanup</bold></red>"), subject,
                confirmed -> executeCleanup(confirmed, query), this::openEntityManagement);
    }

    private void executeCleanup(Player actor, EntityCleanupService.Query query) {
        if (!requireFeature(actor, config.get().admin().entityManagement().enabled()
                && config.get().admin().entityManagement().killall().enabled(), "plexonutility.admin.killall")) return;
        EntityCleanupService.Result result = cleanup.execute(actor, query);
        messages.send(actor, "admin-killall-success", Map.of(
                "count", Integer.toString(result.removed()),
                "protected", Integer.toString(result.protectedCount()),
                "selector", query.selection().canonical(),
                "world", query.world().getName()));
        openEntityManagement(actor);
    }

    private void openSpawnDialog(Player actor) {
        UtilityConfig.EntityManagementConfig entity = config.get().admin().entityManagement();
        if (!requireFeature(actor, entity.enabled() && entity.spawnmob().enabled(), "plexonutility.admin.spawnmob")) return;
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(items.render("<green><bold>Spawn Mob</bold></green>"))
                        .body(List.of(DialogBody.plainMessage(items.render(
                                "<gray>Use a spawnable Bukkit entity type and an amount within the configured hard cap.</gray>"))))
                        .inputs(List.of(
                                DialogInput.text("entity", Component.text("Entity")).initial("zombie").maxLength(64).width(300).build(),
                                DialogInput.text("amount", Component.text("Amount")).initial("1").maxLength(3).width(300).build()))
                        .build())
                .type(DialogType.notice(ActionButton.create(items.render("<green>Spawn</green>"), Component.empty(), 160,
                        DialogAction.customClick((view, audience) -> {
                            if (!(audience instanceof Player player)) return;
                            String type = view.getText("entity");
                            String amount = view.getText("amount");
                            player.closeDialog();
                            scheduler.runPrimary(() -> spawn(player, type, amount));
                        }, DIALOG_CALLBACK)))));
        actor.showDialog(dialog);
    }

    private void spawn(Player actor, String rawType, String rawAmount) {
        UtilityConfig.EntityManagementConfig entity = config.get().admin().entityManagement();
        if (!requireFeature(actor, entity.enabled() && entity.spawnmob().enabled(), "plexonutility.admin.spawnmob")) return;
        final EntityType type;
        try {
            type = spawning.parseType(rawType == null ? "" : rawType.trim());
        } catch (IllegalArgumentException exception) {
            messages.send(actor, "admin-spawnmob-invalid-type", Map.of("type", String.valueOf(rawType)));
            return;
        }
        int amount;
        try {
            amount = Integer.parseInt(rawAmount == null ? "" : rawAmount.trim());
        } catch (NumberFormatException exception) {
            messages.send(actor, "admin-spawnmob-invalid-amount", Map.of("amount", String.valueOf(rawAmount)));
            return;
        }
        int max = entity.spawnmob().maxAmount();
        if (amount < 1 || amount > max || amount > 100) {
            messages.send(actor, "admin-spawnmob-invalid-amount", Map.of("amount", Integer.toString(amount), "max", Integer.toString(max)));
            return;
        }
        EntitySpawnService.SpawnResult result = spawning.spawn(actor, actor, type, amount);
        if (result.location() == null) {
            messages.send(actor, "admin-spawnmob-no-safe-location");
        } else if (result.failed() > 0) {
            messages.send(actor, "admin-spawnmob-partial", Map.of(
                    "spawned", Integer.toString(result.spawned()), "requested", Integer.toString(result.requested()),
                    "failed", Integer.toString(result.failed()), "type", type.name().toLowerCase(Locale.ROOT)));
        } else {
            messages.send(actor, "admin-spawnmob-success", Map.of(
                    "count", Integer.toString(result.spawned()), "type", type.name().toLowerCase(Locale.ROOT)));
        }
        openEntityManagement(actor);
    }

    private void setGameMode(Player actor, UUID targetId, GameMode mode) {
        Player target = online(targetId, actor);
        if (target == null || !targetPermission(actor, target, config.get().admin().playerManagement().gamemodeEnabled(),
                "plexonutility.admin.gamemode", "plexonutility.admin.gamemode.others")) return;
        players.setGameMode(actor, target, mode);
        messages.send(actor, "admin-gamemode-success", Map.of("player", target.getName(), "mode", mode.name().toLowerCase(Locale.ROOT)));
        openPlayerManagement(actor, targetId);
    }

    private void toggleFlight(Player actor, UUID targetId) {
        Player target = online(targetId, actor);
        if (target == null || !targetPermission(actor, target, config.get().admin().playerManagement().flyEnabled(),
                "plexonutility.admin.fly", "plexonutility.admin.fly.others")) return;
        if (!players.canManageFlight(target)) {
            messages.send(actor, "admin-fly-gamemode-managed", Map.of("player", target.getName()));
            return;
        }
        boolean enabled = players.toggleFlight(actor, target);
        messages.send(actor, enabled ? "admin-fly-on" : "admin-fly-off", Map.of("player", target.getName()));
        openPlayerManagement(actor, targetId);
    }

    private void toggleGod(Player actor, UUID targetId) {
        Player target = online(targetId, actor);
        if (target == null || !targetPermission(actor, target, config.get().admin().playerManagement().godEnabled(),
                "plexonutility.admin.god", "plexonutility.admin.god.others")) return;
        boolean enabled = players.toggleGodMode(actor, target);
        messages.send(actor, enabled ? "admin-god-on" : "admin-god-off", Map.of("player", target.getName()));
        openPlayerManagement(actor, targetId);
    }

    private void setSpeed(Player actor, UUID targetId, PlayerManagementService.SpeedMode mode, int level) {
        Player target = online(targetId, actor);
        if (target == null || !targetPermission(actor, target, config.get().admin().playerManagement().speedEnabled(),
                "plexonutility.admin.speed", "plexonutility.admin.speed.others")) return;
        players.setSpeed(actor, target, mode, level);
        messages.send(actor, "admin-speed-success", Map.of(
                "player", target.getName(), "mode", mode.name().toLowerCase(Locale.ROOT), "speed", Integer.toString(level)));
        openSpeed(actor, targetId);
    }

    private void resetSpeed(Player actor, UUID targetId, PlayerManagementService.SpeedMode mode) {
        Player target = online(targetId, actor);
        if (target == null || !targetPermission(actor, target, config.get().admin().playerManagement().speedEnabled(),
                "plexonutility.admin.speed", "plexonutility.admin.speed.others")) return;
        players.resetSpeed(actor, target, mode);
        messages.send(actor, "admin-speed-reset", Map.of("player", target.getName(), "mode", mode.name().toLowerCase(Locale.ROOT)));
        openSpeed(actor, targetId);
    }

    private void inspect(Player actor, UUID targetId) {
        Player target = online(targetId, actor);
        if (target == null || !targetPermission(actor, target, config.get().admin().inventoryInspectionEnabled(),
                "plexonutility.admin.invsee", "plexonutility.admin.invsee")) return;
        inventory.open(actor, target);
    }

    private void confirmClearInventory(Player actor, UUID targetId) {
        Player target = online(targetId, actor);
        if (target == null || !targetPermission(actor, target, config.get().admin().playerManagement().clearInventoryEnabled(),
                "plexonutility.admin.clearinventory", "plexonutility.admin.clearinventory.others")) return;
        ItemStack subject = items.head(target,
                items.template("<red><bold>Clear <player>'s Inventory?</bold></red>", Map.of("player", target.getName())),
                List.of(items.render("<gray>Storage, armor and offhand will be cleared.</gray>"),
                        items.render("<red>This cannot be undone by PlexonUtility.</red>")));
        gui.confirmation(actor, "utility", "clearinventory-confirm-3.5", items.render("<red><bold>Confirm Inventory Clear</bold></red>"),
                subject, confirmed -> clearInventory(confirmed, targetId), cancelled -> openInventoryManagement(cancelled, targetId));
    }

    private void clearInventory(Player actor, UUID targetId) {
        Player target = online(targetId, actor);
        if (target == null || !targetPermission(actor, target, config.get().admin().playerManagement().clearInventoryEnabled(),
                "plexonutility.admin.clearinventory", "plexonutility.admin.clearinventory.others")) return;
        int stacks = players.clearInventory(actor, target);
        messages.send(actor, "admin-clearinventory-success", Map.of("player", target.getName(), "count", Integer.toString(stacks)));
        openInventoryManagement(actor, targetId);
    }

    private void openAnvil(Player actor) {
        if (!requireFeature(actor, config.get().admin().playerManagement().anvilEnabled(), "plexonutility.anvil")) return;
        actor.openAnvil(null, true);
    }

    private boolean targetPermission(Player actor, Player target, boolean featureEnabled, String selfPermission, String othersPermission) {
        if (!requireFeature(actor, featureEnabled, selfPermission)) return false;
        if (actor.getUniqueId().equals(target.getUniqueId())) return true;
        return require(actor, othersPermission);
    }

    private boolean playerManagementEnabled() {
        UtilityConfig.PlayerManagementConfig policy = config.get().admin().playerManagement();
        return policy.gamemodeEnabled() || policy.flyEnabled() || policy.godEnabled() || policy.speedEnabled()
                || policy.clearInventoryEnabled() || policy.anvilEnabled();
    }

    private Player online(UUID id, Player feedbackTarget) {
        Player current = Bukkit.getPlayer(id);
        if (current == null || !current.isOnline() || !current.isConnected()) {
            messages.send(feedbackTarget, "admin-target-offline");
            return null;
        }
        return current;
    }

    private boolean requireFeature(Player player, boolean enabled, String permission) {
        if (!config.get().admin().enabled() || !enabled) {
            messages.send(player, "admin-feature-disabled");
            return false;
        }
        return require(player, permission);
    }

    private boolean require(Player player, String permission) {
        if (player.hasPermission(permission)) return true;
        messages.send(player, "no-permission");
        return false;
    }

    private ItemStack playerIcon(Player viewer, Player target) {
        List<Component> lore = new ArrayList<>();
        lore.add(items.template("<gray>World:</gray> <white><world></white>", Map.of("world", target.getWorld().getName())));
        lore.add(items.template("<gray>Mode:</gray> <white><mode></white>", Map.of("mode", target.getGameMode().name())));
        lore.add(items.render(players.isGodMode(target.getUniqueId()) ? "<gray>God:</gray> <yellow>ON</yellow>" : "<gray>God:</gray> <green>OFF</green>"));
        lore.add(items.render(players.isFlightManaged(target.getUniqueId()) ? "<gray>Managed flight:</gray> <yellow>ON</yellow>" : "<gray>Managed flight:</gray> <green>OFF</green>"));
        if (viewer.getUniqueId().equals(target.getUniqueId())) lore.add(items.render("<gray>Target:</gray> <aqua>YOU</aqua>"));
        lore.add(items.render(""));
        lore.add(items.render("<yellow>Click</yellow> <gray>to manage.</gray>"));
        return items.head(target, items.template("<aqua><bold><player></bold></aqua>", Map.of("player", target.getName())), lore);
    }

    private ItemStack playerStateIcon(Player target) {
        return items.head(target, items.template("<aqua><bold><player></bold></aqua>", Map.of("player", target.getName())), List.of(
                items.template("<gray>Mode:</gray> <white><mode></white>", Map.of("mode", target.getGameMode().name())),
                items.render(players.isGodMode(target.getUniqueId()) ? "<gray>God:</gray> <yellow>ON</yellow>" : "<gray>God:</gray> <green>OFF</green>"),
                items.render(players.isFlightManaged(target.getUniqueId()) ? "<gray>Managed flight:</gray> <yellow>ON</yellow>" : "<gray>Managed flight:</gray> <green>OFF</green>"),
                items.template("<gray>Walk/Fly speed:</gray> <white><walk> / <fly></white>", Map.of(
                        "walk", target.getWalkSpeed(), "fly", target.getFlySpeed()))));
    }

    private ItemStack entityPolicyIcon() {
        UtilityConfig.EntityManagementConfig entity = config.get().admin().entityManagement();
        return items.icon(Material.COMPARATOR, "<aqua><bold>Safety Policy</bold></aqua>", List.of(
                "<gray>Max cleanup radius:</gray> <white>" + entity.killall().maxRadius() + "</white>",
                "<gray>Command confirm threshold:</gray> <white>" + entity.killall().confirmationThreshold() + "</white>",
                "<gray>Max spawn batch:</gray> <white>" + entity.spawnmob().maxAmount() + "</white>",
                "",
                "<dark_gray>GUI cleanup always requires confirmation.</dark_gray>"));
    }

    private ItemStack actionIcon(Player viewer, Material material, String name, String description, String permission, boolean featureEnabled) {
        boolean permitted = viewer.hasPermission(permission);
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + description + "</gray>");
        lore.add("");
        lore.add(featureEnabled ? "<gray>Status:</gray> <green>AVAILABLE</green>" : "<gray>Status:</gray> <dark_gray>DISABLED</dark_gray>");
        lore.add(permitted ? "<gray>Permission:</gray> <green>GRANTED</green>" : "<gray>Permission:</gray> <red>NOT GRANTED</red>");
        lore.add("");
        lore.add(featureEnabled && permitted ? "<yellow>Click</yellow> <gray>to continue.</gray>" : "<dark_gray>Action unavailable.</dark_gray>");
        return items.icon(material, name, lore);
    }

    private ItemStack modeIcon(Material material, String label, boolean selected) {
        return items.icon(material, "<green><bold>" + label + "</bold></green>", List.of(
                selected ? "<gray>Status:</gray> <green>SELECTED</green>" : "<gray>Status:</gray> <dark_gray>Not selected</dark_gray>",
                "", "<yellow>Click</yellow> <gray>to apply.</gray>"));
    }

    private ItemStack speedIcon(String label, float current, int level) {
        return items.icon(Material.SUGAR, "<gold><bold>" + label + "</bold></gold>", List.of(
                "<gray>Current raw value:</gray> <white>" + current + "</white>",
                "<gray>Preset level:</gray> <white>" + level + "/10</white>",
                "", "<yellow>Click</yellow> <gray>to apply.</gray>"));
    }

    private ItemStack resetSpeedIcon(String label) {
        return items.icon(Material.MILK_BUCKET, "<aqua><bold>" + label + "</bold></aqua>",
                List.of("<gray>Restore the vanilla-compatible Utility default.</gray>", "", "<yellow>Click</yellow> <gray>to reset.</gray>"));
    }

    private ItemStack pageStatusIcon(int page, int pages, int count) {
        return items.icon(Material.PAPER, "<aqua><bold>Page " + (page + 1) + "/" + pages + "</bold></aqua>", List.of(
                "<gray>Online players:</gray> <white>" + count + "</white>",
                "", "<yellow>Click</yellow> <gray>to refresh.</gray>"));
    }

    private ItemStack pageIcon(String label) {
        return items.icon(Material.ARROW, "<aqua><bold>" + label + "</bold></aqua>", List.of());
    }

    private ItemStack backIcon() {
        return items.icon(Material.ARROW, "<aqua><bold>Back</bold></aqua>", List.of("<yellow>Click</yellow> <gray>to return.</gray>"));
    }

    private ItemStack refreshIcon() {
        return items.icon(Material.SUNFLOWER, "<aqua><bold>Refresh</bold></aqua>", List.of("<yellow>Click</yellow> <gray>to refresh live state.</gray>"));
    }

    private ItemStack closeIcon() {
        return items.icon(Material.BARRIER, "<red><bold>Close</bold></red>", List.of("<yellow>Click</yellow> <gray>to close.</gray>"));
    }

    private void addFrame(GuiService.GuiBuilder builder, int rows) {
        for (int row = 0; row < rows - 1; row++) {
            builder.button(row * 9, items.icon(Material.LIME_STAINED_GLASS_PANE, " ", List.of()), click -> { });
            builder.button(row * 9 + 8, items.icon(Material.CYAN_STAINED_GLASS_PANE, " ", List.of()), click -> { });
        }
    }

    public enum SelectorContext {
        CONTROLS,
        INVENTORY
    }
}
