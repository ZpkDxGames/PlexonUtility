package com.zpkdxgames.plexonutility.admin.gui;

import com.zpkdxgames.plexoncore.gui.GuiService;
import com.zpkdxgames.plexoncore.scheduler.CoreScheduler;
import com.zpkdxgames.plexonutility.PlexonUtilityPlugin;
import com.zpkdxgames.plexonutility.admin.inventory.InventoryInspectionService;
import com.zpkdxgames.plexonutility.admin.moderation.DurationParser;
import com.zpkdxgames.plexonutility.admin.moderation.ModerationService;
import com.zpkdxgames.plexonutility.admin.prison.PrisonLocation;
import com.zpkdxgames.plexonutility.admin.prison.PrisonService;
import com.zpkdxgames.plexonutility.admin.vanish.VanishService;
import com.zpkdxgames.plexonutility.afk.AfkManager;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.menu.UtilityMenuItemFactory;
import com.zpkdxgames.plexonutility.menu.UtilityMenuService;
import com.zpkdxgames.plexonutility.message.MessageService;
import com.zpkdxgames.plexonutility.service.PlayerUtilityService;
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
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/** Core GUI navigation plus Paper Dialog typed input for the focused 3.3 staff control plane. */
public final class AdminMenuService {
    private static final int PAGE_SIZE = 45;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final ClickCallback.Options DIALOG_CALLBACK = ClickCallback.Options.builder()
            .uses(1).lifetime(Duration.ofMinutes(5)).build();

    private final PlexonUtilityPlugin plugin;
    private final Supplier<UtilityConfig> config;
    private final MessageService messages;
    private final GuiService gui;
    private final CoreScheduler scheduler;
    private final UtilityMenuItemFactory items;
    private final UtilityMenuService utilityMenu;
    private final AfkManager afk;
    private final VanishService vanish;
    private final ModerationService moderation;
    private final PrisonService prison;
    private final InventoryInspectionService inventory;
    private final PlayerUtilityService utilities;

    public AdminMenuService(PlexonUtilityPlugin plugin, Supplier<UtilityConfig> config, MessageService messages,
                            GuiService gui, CoreScheduler scheduler, UtilityMenuItemFactory items,
                            UtilityMenuService utilityMenu, AfkManager afk, VanishService vanish,
                            ModerationService moderation, PrisonService prison,
                            InventoryInspectionService inventory, PlayerUtilityService utilities) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.gui = gui;
        this.scheduler = scheduler;
        this.items = items;
        this.utilityMenu = utilityMenu;
        this.afk = afk;
        this.vanish = vanish;
        this.moderation = moderation;
        this.prison = prison;
        this.inventory = inventory;
        this.utilities = utilities;
    }

    public void open(Player player) {
        if (!requireFeature(player, config.get().admin().enabled(), "plexonutility.admin.menu")) return;
        var builder = gui.builder("utility", "admin-center-3.3",
                        items.render("<gradient:#57E389:#22D3EE><bold>Admin Center</bold></gradient>"), 5)
                .filler(Material.BLACK_STAINED_GLASS_PANE);
        addFrame(builder, 5);
        builder.button(4, adminProfile(player), click -> open(click.player()));
        builder.button(10, actionIcon(player, Material.PLAYER_HEAD, "<aqua><bold>Player Management</bold></aqua>",
                "Browse online players and open focused actions.", "plexonutility.admin.menu", true),
                click -> openPlayerSelector(click.player(), 0, SelectorContext.ACTIONS));
        builder.button(12, actionIcon(player, vanish.isVanished(player.getUniqueId()) ? Material.ENDER_EYE : Material.ENDER_PEARL,
                "<yellow><bold>Vanish</bold></yellow>", "Toggle native staff self-vanish.",
                "plexonutility.admin.vanish", config.get().admin().vanish().enabled()), click -> toggleVanish(click.player()));
        builder.button(14, actionIcon(player, Material.IRON_BARS, "<gold><bold>Prison</bold></gold>",
                "Manage the holding-location waypoint.", "plexonutility.admin.prison", config.get().admin().prisonEnabled()),
                click -> openPrison(click.player()));
        builder.button(16, actionIcon(player, Material.WRITABLE_BOOK, "<red><bold>Ban Management</bold></red>",
                "Review active native profile bans.", "plexonutility.admin.unban", config.get().admin().moderation().banEnabled()),
                click -> openBanManagement(click.player(), 0));
        builder.button(20, items.icon(Material.BEACON, "<aqua><bold>PlexonFamily / Integrations</bold></aqua>", List.of(
                "<gray>Open the existing lifecycle-driven integration view.</gray>", "", "<yellow>Click</yellow> <gray>to open.</gray>")),
                click -> utilityMenu.openFamily(click.player()));
        builder.button(22, items.icon(Material.COMPARATOR, "<aqua><bold>Diagnostics</bold></aqua>", List.of(
                "<gray>Open the existing Utility runtime diagnostics.</gray>", "", "<yellow>Click</yellow> <gray>to inspect.</gray>")),
                click -> utilityMenu.openDiagnostics(click.player()));
        builder.button(24, actionIcon(player, Material.REPEATER, "<yellow><bold>Reload</bold></yellow>",
                "Atomically reload validated Utility configuration.", "plexonutility.reload", true),
                click -> reload(click.player()));
        builder.button(36, backIcon(), click -> utilityMenu.open(click.player()));
        builder.button(40, refreshIcon(), click -> open(click.player()));
        builder.button(44, closeIcon(), click -> click.player().closeInventory());
        builder.open(player);
    }

    public void openPlayerSelector(Player viewer, int requestedPage, SelectorContext context) {
        String permission = context == SelectorContext.PRISON_SEND ? "plexonutility.admin.prison.send" : "plexonutility.admin.menu";
        if (!require(viewer, permission)) return;
        List<Player> online = Bukkit.getOnlinePlayers().stream()
                .filter(Player::isConnected)
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
        int pages = Math.max(1, (online.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        int start = page * PAGE_SIZE;
        int end = Math.min(online.size(), start + PAGE_SIZE);
        var builder = gui.builder("utility", "player-selector-3.3-" + context.name().toLowerCase(),
                        items.render(context == SelectorContext.PRISON_SEND
                                ? "<gold><bold>Select Prison Target</bold></gold>"
                                : "<aqua><bold>Player Management</bold></aqua>"), 6)
                .filler(Material.BLACK_STAINED_GLASS_PANE).page(page);
        for (int index = start; index < end; index++) {
            Player target = online.get(index);
            builder.button(index - start, playerSelectorIcon(viewer, target), click -> {
                Player current = online(target.getUniqueId(), click.player());
                if (current == null) {
                    openPlayerSelector(click.player(), page, context);
                    return;
                }
                if (context == SelectorContext.PRISON_SEND) executePrisonSend(click.player(), current);
                else openPlayerActions(click.player(), current.getUniqueId());
            });
        }
        builder.button(45, backIcon(), click -> {
            if (context == SelectorContext.PRISON_SEND) openPrison(click.player());
            else open(click.player());
        });
        if (page > 0) builder.button(47, pageIcon("Previous Page"), click -> openPlayerSelector(click.player(), page - 1, context));
        builder.button(49, pageStatusIcon(page, pages, online.size(), "Online players"), click -> openPlayerSelector(click.player(), page, context));
        if (page + 1 < pages) builder.button(51, pageIcon("Next Page"), click -> openPlayerSelector(click.player(), page + 1, context));
        builder.button(53, closeIcon(), click -> click.player().closeInventory());
        builder.open(viewer);
    }

    public void openPlayerActions(Player viewer, UUID targetId) {
        if (!require(viewer, "plexonutility.admin.menu")) return;
        Player target = online(targetId, viewer);
        if (target == null) {
            openPlayerSelector(viewer, 0, SelectorContext.ACTIONS);
            return;
        }
        var builder = gui.builder("utility", "player-actions-3.3",
                        items.template("<aqua><bold>Manage <player></bold></aqua>", Map.of("player", target.getName())), 5)
                .filler(Material.BLACK_STAINED_GLASS_PANE);
        addFrame(builder, 5);
        builder.button(4, targetProfile(target), click -> openPlayerActions(click.player(), targetId));
        builder.button(10, playerInfo(target), click -> { });
        builder.button(11, actionIcon(viewer, Material.CHEST, "<aqua><bold>Inventory Inspect</bold></aqua>",
                "Open a cloned, read-only inventory snapshot.", "plexonutility.admin.invsee", config.get().admin().inventoryInspectionEnabled()),
                click -> inspectInventory(click.player(), targetId));
        builder.button(12, actionIcon(viewer, Material.ENDER_CHEST, "<aqua><bold>Ender Chest</bold></aqua>",
                "Reuse the existing target Ender Chest capability.", "plexonutility.enderchest.others", config.get().enabled(Feature.ENDERCHEST)),
                click -> openEnderChest(click.player(), targetId));
        builder.button(14, actionIcon(viewer, Material.GLISTERING_MELON_SLICE, "<green><bold>Heal</bold></green>",
                "Restore the selected player's health.", "plexonutility.heal.others", config.get().enabled(Feature.HEAL)),
                click -> heal(click.player(), targetId));
        builder.button(15, actionIcon(viewer, Material.GOLDEN_CARROT, "<green><bold>Feed</bold></green>",
                "Restore the selected player's hunger.", "plexonutility.feed.others", config.get().enabled(Feature.FEED)),
                click -> feed(click.player(), targetId));
        builder.button(19, actionIcon(viewer, Material.COMPASS, "<gold><bold>Teleport to Player</bold></gold>",
                "Administrative movement to the selected player.", "plexonutility.admin.teleport", true),
                click -> teleportTo(click.player(), targetId));
        builder.button(20, actionIcon(viewer, Material.ENDER_PEARL, "<gold><bold>Bring Player</bold></gold>",
                "Bring the selected player to your location.", "plexonutility.admin.teleport", true),
                click -> bring(click.player(), targetId));
        builder.button(22, actionIcon(viewer, Material.IRON_BARS, "<gold><bold>Send to Prison</bold></gold>",
                "Teleport the selected player to the holding waypoint.", "plexonutility.admin.prison.send", config.get().admin().prisonEnabled()),
                click -> {
                    Player actor = click.player();
                    Player current = online(targetId, actor);
                    if (current != null) executePrisonSend(actor, current);
                });
        builder.button(24, actionIcon(viewer, Material.IRON_DOOR, "<red><bold>Kick</bold></red>",
                "Enter a reason, then confirm removal.", "plexonutility.admin.kick", config.get().admin().moderation().kickEnabled()),
                click -> startKick(click.player(), targetId));
        builder.button(25, actionIcon(viewer, Material.REDSTONE_BLOCK, "<red><bold>Ban</bold></red>",
                "Enter duration/reason, then confirm the profile ban.", "plexonutility.admin.ban", config.get().admin().moderation().banEnabled()),
                click -> startBan(click.player(), targetId));
        builder.button(36, backIcon(), click -> openPlayerSelector(click.player(), 0, SelectorContext.ACTIONS));
        builder.button(40, refreshIcon(), click -> openPlayerActions(click.player(), targetId));
        builder.button(44, closeIcon(), click -> click.player().closeInventory());
        builder.open(viewer);
    }

    public void openPrison(Player player) {
        if (!requireFeature(player, config.get().admin().prisonEnabled(), "plexonutility.admin.prison")) return;
        Optional<PrisonLocation> current = prison.location();
        var builder = gui.builder("utility", "prison-3.3", items.render("<gold><bold>Prison Management</bold></gold>"), 3)
                .filler(Material.BLACK_STAINED_GLASS_PANE);
        builder.button(4, prisonStatusIcon(current), click -> { });
        builder.button(10, actionIcon(player, Material.LODESTONE, "<gold><bold>Set Here</bold></gold>",
                "Save your current location as the holding waypoint.", "plexonutility.admin.prison.set", true),
                click -> setPrison(click.player()));
        builder.button(12, actionIcon(player, Material.COMPASS, "<gold><bold>Go to Prison</bold></gold>",
                "Teleport yourself to the configured waypoint.", "plexonutility.admin.prison.goto", current.isPresent()),
                click -> gotoPrison(click.player()));
        builder.button(14, actionIcon(player, Material.PLAYER_HEAD, "<gold><bold>Send Player</bold></gold>",
                "Choose an online player to send to the waypoint.", "plexonutility.admin.prison.send", current.isPresent()),
                click -> openPlayerSelector(click.player(), 0, SelectorContext.PRISON_SEND));
        builder.button(16, actionIcon(player, Material.BARRIER, "<red><bold>Clear Location</bold></red>",
                "Remove the configured holding waypoint.", "plexonutility.admin.prison.clear", current.isPresent()),
                click -> clearPrison(click.player()));
        builder.button(18, backIcon(), click -> open(click.player()));
        builder.button(22, refreshIcon(), click -> openPrison(click.player()));
        builder.button(26, closeIcon(), click -> click.player().closeInventory());
        builder.open(player);
    }

    public void openBanManagement(Player player, int requestedPage) {
        if (!requireFeature(player, config.get().admin().moderation().banEnabled(), "plexonutility.admin.unban")) return;
        List<ModerationService.BanView> bans = moderation.activeBans();
        int pages = Math.max(1, (bans.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.max(0, Math.min(requestedPage, pages - 1));
        int start = page * PAGE_SIZE;
        int end = Math.min(bans.size(), start + PAGE_SIZE);
        var builder = gui.builder("utility", "ban-management-3.3", items.render("<red><bold>Ban Management</bold></red>"), 6)
                .filler(Material.BLACK_STAINED_GLASS_PANE).page(page);
        for (int index = start; index < end; index++) {
            ModerationService.BanView ban = bans.get(index);
            builder.button(index - start, banIcon(ban), click -> confirmUnban(click.player(), ban, page));
        }
        builder.button(45, backIcon(), click -> open(click.player()));
        if (page > 0) builder.button(47, pageIcon("Previous Page"), click -> openBanManagement(click.player(), page - 1));
        builder.button(49, pageStatusIcon(page, pages, bans.size(), "Active profile bans"), click -> openBanManagement(click.player(), page));
        if (page + 1 < pages) builder.button(51, pageIcon("Next Page"), click -> openBanManagement(click.player(), page + 1));
        builder.button(53, closeIcon(), click -> click.player().closeInventory());
        builder.open(player);
    }

    private void toggleVanish(Player actor) {
        if (!requireFeature(actor, config.get().admin().vanish().enabled(), "plexonutility.admin.vanish")) return;
        boolean state = vanish.toggle(actor, actor);
        actor.sendActionBar(messages.renderUnprefixed(state ? "admin-vanish-on" : "admin-vanish-off", Map.of()));
        open(actor);
    }

    private void reload(Player actor) {
        if (!require(actor, "plexonutility.reload")) return;
        try {
            plugin.reloadUtilityState();
            messages.send(actor, "reloaded");
            open(actor);
        } catch (RuntimeException exception) {
            String reason = exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage();
            messages.send(actor, "reload-failed", Map.of("reason", reason));
        }
    }

    private void inspectInventory(Player actor, UUID targetId) {
        Player target = online(targetId, actor);
        if (target != null && requireFeature(actor, config.get().admin().inventoryInspectionEnabled(), "plexonutility.admin.invsee")) {
            inventory.open(actor, target);
        }
    }

    private void openEnderChest(Player actor, UUID targetId) {
        Player target = online(targetId, actor);
        if (target != null && requireFeature(actor, config.get().enabled(Feature.ENDERCHEST), "plexonutility.enderchest.others")) {
            utilities.openEnderChest(actor, target);
        }
    }

    private void heal(Player actor, UUID targetId) {
        Player target = online(targetId, actor);
        if (target != null && requireFeature(actor, config.get().enabled(Feature.HEAL), "plexonutility.heal.others")) {
            utilities.heal(actor, target, false);
            openPlayerActions(actor, targetId);
        }
    }

    private void feed(Player actor, UUID targetId) {
        Player target = online(targetId, actor);
        if (target != null && requireFeature(actor, config.get().enabled(Feature.FEED), "plexonutility.feed.others")) {
            utilities.feed(actor, target, false);
            openPlayerActions(actor, targetId);
        }
    }

    private void teleportTo(Player actor, UUID targetId) {
        Player target = online(targetId, actor);
        if (target != null && require(actor, "plexonutility.admin.teleport")) actor.teleport(target.getLocation());
    }

    private void bring(Player actor, UUID targetId) {
        Player target = online(targetId, actor);
        if (target != null && require(actor, "plexonutility.admin.teleport")) target.teleport(actor.getLocation());
    }

    private void startKick(Player actor, UUID targetId) {
        Player target = online(targetId, actor);
        if (target == null || !requireFeature(actor, config.get().admin().moderation().kickEnabled(), "plexonutility.admin.kick")) return;
        if (actor.getUniqueId().equals(targetId)) {
            messages.send(actor, "admin-self-action-denied");
            return;
        }
        openKickDialog(actor, target);
    }

    private void startBan(Player actor, UUID targetId) {
        Player target = online(targetId, actor);
        if (target == null || !requireFeature(actor, config.get().admin().moderation().banEnabled(), "plexonutility.admin.ban")) return;
        if (actor.getUniqueId().equals(targetId)) {
            messages.send(actor, "admin-self-action-denied");
            return;
        }
        openBanDialog(actor, target);
    }

    private void setPrison(Player actor) {
        if (!require(actor, "plexonutility.admin.prison.set")) return;
        if (prison.location().isPresent()) {
            gui.confirmation(actor, "utility", "prison-set-confirm-3.3", items.render("<red><bold>Overwrite Prison?</bold></red>"),
                    prisonStatusIcon(prison.location()), confirmed -> {
                        PrisonLocation set = prison.set(confirmed);
                        messages.send(confirmed, "admin-prison-set", Map.of("world", set.world(), "coordinates", set.coordinates()));
                        openPrison(confirmed);
                    }, this::openPrison);
            return;
        }
        PrisonLocation set = prison.set(actor);
        messages.send(actor, "admin-prison-set", Map.of("world", set.world(), "coordinates", set.coordinates()));
        openPrison(actor);
    }

    private void gotoPrison(Player actor) {
        if (!require(actor, "plexonutility.admin.prison.goto")) return;
        prisonFeedback(actor, prison.gotoPrison(actor), null);
        openPrison(actor);
    }

    private void clearPrison(Player actor) {
        if (!require(actor, "plexonutility.admin.prison.clear")) return;
        if (prison.location().isEmpty()) {
            messages.send(actor, "admin-prison-not-configured");
            return;
        }
        gui.confirmation(actor, "utility", "prison-clear-confirm-3.3", items.render("<red><bold>Clear Prison Location?</bold></red>"),
                prisonStatusIcon(prison.location()), confirmed -> {
                    prison.clear(confirmed);
                    messages.send(confirmed, "admin-prison-cleared");
                    openPrison(confirmed);
                }, this::openPrison);
    }

    private void executePrisonSend(Player actor, Player target) {
        if (!requireFeature(actor, config.get().admin().prisonEnabled(), "plexonutility.admin.prison.send")) return;
        PrisonService.Result result = prison.send(actor, target);
        prisonFeedback(actor, result, target.getName());
        if (result == PrisonService.Result.SUCCESS) openPlayerActions(actor, target.getUniqueId());
        else openPrison(actor);
    }

    private void openKickDialog(Player actor, Player target) {
        Component title = items.template("<red><bold>Kick <player></bold></red>", Map.of("player", target.getName()));
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(DialogBody.plainMessage(items.render("<gray>Enter the removal reason. A final confirmation follows.</gray>"))))
                        .inputs(List.of(DialogInput.text("reason", Component.text("Reason"))
                                .initial(config.get().admin().moderation().kickDefaultReason())
                                .maxLength(ModerationService.MAX_REASON_LENGTH).width(300).build()))
                        .build())
                .type(DialogType.notice(ActionButton.create(items.render("<yellow>Continue</yellow>"), Component.empty(), 160,
                        DialogAction.customClick((view, audience) -> {
                            if (!(audience instanceof Player player)) return;
                            String reason = view.getText("reason");
                            player.closeDialog();
                            scheduler.runPrimary(() -> confirmKick(player, target.getUniqueId(), reason));
                        }, DIALOG_CALLBACK)))));
        actor.showDialog(dialog);
    }

    private void confirmKick(Player actor, UUID targetId, String rawReason) {
        Player target = online(targetId, actor);
        if (target == null || !requireFeature(actor, config.get().admin().moderation().kickEnabled(), "plexonutility.admin.kick")) return;
        if (actor.getUniqueId().equals(targetId)) {
            messages.send(actor, "admin-self-action-denied");
            return;
        }
        final String reason;
        try {
            reason = moderation.normalizeReason(rawReason, config.get().admin().moderation().kickDefaultReason());
        } catch (IllegalArgumentException exception) {
            messages.send(actor, "admin-invalid-reason", Map.of("reason", exception.getMessage()));
            return;
        }
        ItemStack subject = items.head(target,
                items.template("<red><bold>Kick <player></bold></red>", Map.of("player", target.getName())),
                List.of(items.template("<gray>Reason:</gray> <white><reason></white>", Map.of("reason", reason))));
        gui.confirmation(actor, "utility", "kick-confirm-3.3", items.render("<red><bold>Confirm Kick</bold></red>"), subject,
                confirmed -> {
                    Player current = online(targetId, confirmed);
                    if (current == null || !requireFeature(confirmed, config.get().admin().moderation().kickEnabled(), "plexonutility.admin.kick")) return;
                    moderation.kick(confirmed, current, reason, config.get().admin().moderation().kickDefaultReason());
                    messages.send(confirmed, "admin-kick-success", Map.of("player", current.getName()));
                    openPlayerSelector(confirmed, 0, SelectorContext.ACTIONS);
                }, cancelled -> openPlayerActions(cancelled, targetId));
    }

    private void openBanDialog(Player actor, OfflinePlayer target) {
        Component title = items.template("<red><bold>Ban <player></bold></red>", Map.of("player", display(target)));
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .body(List.of(DialogBody.plainMessage(items.render("<gray>Use perm/permanent or a bounded value such as 30m, 2h, 1d, 7d, 30d.</gray>"))))
                        .inputs(List.of(
                                DialogInput.text("duration", Component.text("Duration")).initial("perm").maxLength(16).width(300).build(),
                                DialogInput.text("reason", Component.text("Reason"))
                                        .initial(config.get().admin().moderation().banDefaultReason())
                                        .maxLength(ModerationService.MAX_REASON_LENGTH).width(300).build()))
                        .build())
                .type(DialogType.notice(ActionButton.create(items.render("<yellow>Continue</yellow>"), Component.empty(), 160,
                        DialogAction.customClick((view, audience) -> {
                            if (!(audience instanceof Player player)) return;
                            String duration = view.getText("duration");
                            String reason = view.getText("reason");
                            player.closeDialog();
                            scheduler.runPrimary(() -> confirmBan(player, target, duration, reason));
                        }, DIALOG_CALLBACK)))));
        actor.showDialog(dialog);
    }

    private void confirmBan(Player actor, OfflinePlayer target, String rawDuration, String rawReason) {
        if (!requireFeature(actor, config.get().admin().moderation().banEnabled(), "plexonutility.admin.ban")) return;
        if (actor.getUniqueId().equals(target.getUniqueId())) {
            messages.send(actor, "admin-self-action-denied");
            return;
        }
        final DurationParser.ParsedDuration duration;
        final String reason;
        try {
            duration = DurationParser.parse(rawDuration, config.get().admin().moderation().maxDurationDays());
        } catch (IllegalArgumentException exception) {
            messages.send(actor, "admin-invalid-duration", Map.of("reason", exception.getMessage()));
            return;
        }
        try {
            reason = moderation.normalizeReason(rawReason, config.get().admin().moderation().banDefaultReason());
        } catch (IllegalArgumentException exception) {
            messages.send(actor, "admin-invalid-reason", Map.of("reason", exception.getMessage()));
            return;
        }
        ItemStack subject = items.head(target,
                items.template("<red><bold>Ban <player></bold></red>", Map.of("player", display(target))),
                List.of(
                        items.template("<gray>Duration:</gray> <white><duration></white>", Map.of("duration", duration.display())),
                        items.template("<gray>Reason:</gray> <white><reason></white>", Map.of("reason", reason)),
                        items.template("<gray>Actor:</gray> <white><actor></white>", Map.of("actor", actor.getName()))));
        gui.confirmation(actor, "utility", "ban-confirm-3.3", items.render("<red><bold>Confirm Profile Ban</bold></red>"), subject,
                confirmed -> {
                    if (!requireFeature(confirmed, config.get().admin().moderation().banEnabled(), "plexonutility.admin.ban")) return;
                    moderation.ban(confirmed, target, duration, reason, config.get().admin().moderation().banDefaultReason());
                    messages.send(confirmed, "admin-ban-success", Map.of("player", display(target), "duration", duration.display()));
                    openPlayerSelector(confirmed, 0, SelectorContext.ACTIONS);
                }, cancelled -> {
                    Player current = Bukkit.getPlayer(target.getUniqueId());
                    if (current != null) openPlayerActions(cancelled, current.getUniqueId());
                    else openPlayerSelector(cancelled, 0, SelectorContext.ACTIONS);
                });
    }

    private void confirmUnban(Player actor, ModerationService.BanView ban, int page) {
        if (!require(actor, "plexonutility.admin.unban")) return;
        gui.confirmation(actor, "utility", "unban-confirm-3.3", items.render("<red><bold>Confirm Unban</bold></red>"), banIcon(ban),
                confirmed -> {
                    if (!require(confirmed, "plexonutility.admin.unban")) return;
                    ModerationService.ActionResult result = moderation.unbanProfile(confirmed, ban.profile());
                    if (result == ModerationService.ActionResult.NOT_BANNED) messages.send(confirmed, "admin-not-banned");
                    else messages.send(confirmed, "admin-unban-success", Map.of("player", ban.label()));
                    openBanManagement(confirmed, page);
                }, cancelled -> openBanManagement(cancelled, page));
    }

    private void prisonFeedback(Player actor, PrisonService.Result result, String target) {
        switch (result) {
            case SUCCESS -> {
                if (target == null) messages.send(actor, "admin-prison-goto");
                else messages.send(actor, "admin-prison-sent", Map.of("player", target));
            }
            case NOT_CONFIGURED -> messages.send(actor, "admin-prison-not-configured");
            case WORLD_MISSING -> messages.send(actor, "admin-prison-world-missing");
            case TARGET_OFFLINE -> messages.send(actor, "admin-target-offline");
            case TELEPORT_FAILED -> messages.send(actor, "admin-prison-teleport-failed");
        }
    }

    private ItemStack adminProfile(Player player) {
        return items.head(player, items.render("<gradient:#57E389:#22D3EE><bold>Admin Profile</bold></gradient>"), List.of(
                items.template("<gray>Player:</gray> <white><player></white>", Map.of("player", player.getName())),
                items.template("<gray>UUID:</gray> <white><uuid></white>", Map.of("uuid", player.getUniqueId())),
                items.render(player.isOp() ? "<gray>OP:</gray> <green>YES</green>" : "<gray>OP:</gray> <dark_gray>NO</dark_gray>"),
                items.render(vanish.isVanished(player.getUniqueId()) ? "<gray>Vanish:</gray> <yellow>ON</yellow>" : "<gray>Vanish:</gray> <green>OFF</green>"),
                items.render(""), items.render("<yellow>Click</yellow> <gray>to refresh.</gray>")));
    }

    private ItemStack playerSelectorIcon(Player viewer, Player target) {
        List<Component> lore = new ArrayList<>();
        lore.add(items.template("<gray>World:</gray> <white><world></white>", Map.of("world", target.getWorld().getName())));
        lore.add(items.template("<gray>Ping:</gray> <white><ping> ms</white>", Map.of("ping", target.getPing())));
        lore.add(items.render(afk.isAfk(target.getUniqueId()) ? "<gray>AFK:</gray> <yellow>YES</yellow>" : "<gray>AFK:</gray> <green>NO</green>"));
        if (vanish.isVanished(target.getUniqueId())) lore.add(items.render("<gray>Vanish:</gray> <yellow>VANISHED</yellow>"));
        if (viewer.getUniqueId().equals(target.getUniqueId())) lore.add(items.render("<gray>Target:</gray> <aqua>YOU</aqua>"));
        lore.add(items.render(""));
        lore.add(items.render("<yellow>Click</yellow> <gray>to select.</gray>"));
        return items.head(target, items.template("<aqua><bold><player></bold></aqua>", Map.of("player", target.getName())), lore);
    }

    private ItemStack targetProfile(Player target) {
        return items.head(target, items.template("<aqua><bold><player></bold></aqua>", Map.of("player", target.getName())), List.of(
                items.template("<gray>UUID:</gray> <white><uuid></white>", Map.of("uuid", target.getUniqueId())),
                items.template("<gray>World:</gray> <white><world></white>", Map.of("world", target.getWorld().getName())),
                items.render(vanish.isVanished(target.getUniqueId()) ? "<gray>Vanish:</gray> <yellow>ON</yellow>" : "<gray>Vanish:</gray> <green>OFF</green>"),
                items.render(target.isBanned() ? "<gray>Ban:</gray> <red>ACTIVE</red>" : "<gray>Ban:</gray> <green>NONE</green>")));
    }

    private ItemStack playerInfo(Player target) {
        var location = target.getLocation();
        return items.item(Material.MAP, items.render("<aqua><bold>Player Info</bold></aqua>"), List.of(
                items.template("<gray>Name:</gray> <white><player></white>", Map.of("player", target.getName())),
                items.template("<gray>UUID:</gray> <white><uuid></white>", Map.of("uuid", target.getUniqueId())),
                items.template("<gray>World:</gray> <white><world></white>", Map.of("world", target.getWorld().getName())),
                items.template("<gray>XYZ:</gray> <white><coordinates></white>", Map.of("coordinates", Math.round(location.getX()) + ", " + Math.round(location.getY()) + ", " + Math.round(location.getZ()))),
                items.template("<gray>Game mode:</gray> <white><mode></white>", Map.of("mode", target.getGameMode().name())),
                items.template("<gray>Ping:</gray> <white><ping> ms</white>", Map.of("ping", target.getPing())),
                items.template("<gray>Health:</gray> <white><health></white>", Map.of("health", Math.round(target.getHealth()))),
                items.template("<gray>Food:</gray> <white><food></white>", Map.of("food", target.getFoodLevel())),
                items.render(afk.isAfk(target.getUniqueId()) ? "<gray>AFK:</gray> <yellow>YES</yellow>" : "<gray>AFK:</gray> <green>NO</green>"),
                items.render(target.isBanned() ? "<gray>Ban:</gray> <red>ACTIVE</red>" : "<gray>Ban:</gray> <green>NONE</green>"),
                items.render(target.isOp() ? "<gray>OP:</gray> <yellow>YES</yellow>" : "<gray>OP:</gray> <dark_gray>NO</dark_gray>")));
    }

    private ItemStack prisonStatusIcon(Optional<PrisonLocation> current) {
        if (current.isEmpty()) return items.icon(Material.IRON_BARS, "<gold><bold>Prison Status</bold></gold>", List.of(
                "<gray>Status:</gray> <dark_gray>NOT CONFIGURED</dark_gray>", "", "<dark_gray>This is a waypoint, not a sentence engine.</dark_gray>"));
        PrisonLocation location = current.get();
        return items.item(Material.IRON_BARS, items.render("<gold><bold>Prison Status</bold></gold>"), List.of(
                items.render("<gray>Status:</gray> <green>CONFIGURED</green>"),
                items.template("<gray>World:</gray> <white><world></white>", Map.of("world", location.world())),
                items.template("<gray>Coordinates:</gray> <white><coordinates></white>", Map.of("coordinates", location.coordinates())),
                items.template("<gray>Yaw/Pitch:</gray> <white><yaw> / <pitch></white>", Map.of("yaw", Math.round(location.yaw()), "pitch", Math.round(location.pitch()))),
                items.render(""), items.render("<dark_gray>Holding location only; no jail state is stored.</dark_gray>")));
    }

    private ItemStack banIcon(ModerationService.BanView ban) {
        String expiry = ban.permanent() ? "PERMANENT" : DATE.format(Instant.ofEpochMilli(ban.expirationEpochMillis()));
        return items.item(Material.PLAYER_HEAD,
                items.template("<red><bold><player></bold></red>", Map.of("player", ban.label())), List.of(
                        items.template("<gray>Reason:</gray> <white><reason></white>", Map.of("reason", ban.reason())),
                        items.template("<gray>Source:</gray> <white><source></white>", Map.of("source", ban.source())),
                        items.template("<gray>Expires:</gray> <white><expiry></white>", Map.of("expiry", expiry)),
                        items.render(""), items.render("<yellow>Click</yellow> <gray>to review unban.</gray>")));
    }

    private ItemStack actionIcon(Player viewer, Material material, String name, String description, String permission, boolean featureEnabled) {
        boolean permitted = viewer.hasPermission(permission);
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + description + "</gray>");
        lore.add("");
        lore.add(featureEnabled ? "<gray>Status:</gray> <green>AVAILABLE</green>" : "<gray>Status:</gray> <dark_gray>UNAVAILABLE</dark_gray>");
        lore.add(permitted ? "<gray>Permission:</gray> <green>GRANTED</green>" : "<gray>Permission:</gray> <red>NOT GRANTED</red>");
        lore.add("");
        lore.add(featureEnabled && permitted ? "<yellow>Click</yellow> <gray>to continue.</gray>" : "<dark_gray>Action unavailable in the current state.</dark_gray>");
        return items.icon(material, name, lore);
    }

    private ItemStack pageStatusIcon(int page, int pages, int count, String label) {
        return items.item(Material.PAPER, items.render("<aqua><bold>Page " + (page + 1) + "/" + pages + "</bold></aqua>"), List.of(
                items.template("<gray><label>:</gray> <white><count></white>", Map.of("label", label, "count", count)),
                items.render("<yellow>Click</yellow> <gray>to refresh.</gray>")));
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

    private Player online(UUID id, Player feedbackTarget) {
        Player current = Bukkit.getPlayer(id);
        if (current == null || !current.isOnline() || !current.isConnected()) {
            messages.send(feedbackTarget, "admin-target-offline");
            return null;
        }
        return current;
    }

    private boolean requireFeature(Player player, boolean enabled, String permission) {
        if (!enabled) {
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

    private static String display(OfflinePlayer target) {
        return target.getName() == null ? target.getUniqueId().toString() : target.getName();
    }

    public enum SelectorContext {
        ACTIONS,
        PRISON_SEND
    }
}
