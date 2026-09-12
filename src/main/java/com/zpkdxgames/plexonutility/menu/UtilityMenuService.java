package com.zpkdxgames.plexonutility.menu;

import com.zpkdxgames.plexoncore.gui.GuiService;
import com.zpkdxgames.plexoncore.integration.IntegrationRegistry.IntegrationState;
import com.zpkdxgames.plexoncore.text.TextService;
import com.zpkdxgames.plexoncore.text.TextService.TextMode;
import com.zpkdxgames.plexonutility.afk.AfkManager;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.integration.FamilyCompatibilityService;
import com.zpkdxgames.plexonutility.integration.FamilyCompatibilityService.FamilyStatus;
import com.zpkdxgames.plexonutility.message.MessageService;
import com.zpkdxgames.plexonutility.menu.UtilityMenuModel.Availability;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Premium PlexonUtility presentation layer. Core remains the sole protected-GUI router; this class
 * only renders current state and binds one action per button. No animation or polling task is used.
 */
public final class UtilityMenuService implements CommandExecutor {
    private static final String HUB_TITLE = "<gradient:#57E389:#22D3EE><bold>PlexonUtility</bold></gradient> <dark_gray>•</dark_gray> <gray>Control Hub</gray>";
    private static final String FAMILY_TITLE = "<gradient:#57E389:#22D3EE><bold>PlexonFamily</bold></gradient> <dark_gray>•</dark_gray> <gray>Integrations</gray>";
    private static final String HELP_TITLE = "<gradient:#57E389:#22D3EE><bold>PlexonUtility</bold></gradient> <dark_gray>•</dark_gray> <gray>Help</gray>";
    private static final String ADMIN_TITLE = "<gradient:#57E389:#22D3EE><bold>PlexonUtility</bold></gradient> <dark_gray>•</dark_gray> <red>Diagnostics</red>";
    private static final String BRAND_OPEN = "<gradient:#57E389:#22D3EE><bold>";
    private static final String BRAND_CLOSE = "</bold></gradient>";
    private static final int TOTAL_PLAYER_UTILITIES = 7;
    private static final int[] FAMILY_SLOTS = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29};

    private final Supplier<UtilityConfig> config;
    private final MessageService messages;
    private final GuiService gui;
    private final TextService text;
    private final AfkManager afk;
    private final FamilyCompatibilityService family;

    public UtilityMenuService(Supplier<UtilityConfig> config, MessageService messages, GuiService gui, TextService text,
                              AfkManager afk, FamilyCompatibilityService family) {
        this.config = config;
        this.messages = messages;
        this.gui = gui;
        this.text = text;
        this.afk = afk;
        this.family = family;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length != 0) return false;
        if (!config.get().enabled(Feature.MENU)) {
            messages.send(sender, "feature-disabled");
            return true;
        }
        if (!(sender instanceof Player player)) {
            messages.send(sender, "players-only");
            return true;
        }
        open(player);
        return true;
    }

    public void open(Player player) {
        UtilityConfig cfg = config.get();
        var builder = gui.builder("utility", "hub-3.2", text.render(TextMode.MINIMESSAGE, HUB_TITLE), 5)
                .filler(Material.BLACK_STAINED_GLASS_PANE);
        addFrameAccents(builder);

        builder.button(4, profileIcon(player, cfg), click -> {
            family.refresh();
            open(click.player());
        });

        addFeature(builder, player, 10, Feature.FEED, Material.GOLDEN_CARROT,
                "<green><bold>Feed</bold></green>", "Restore your hunger instantly.", "plexonutility.feed", "feed");
        addFeature(builder, player, 11, Feature.HEAL, Material.GLISTERING_MELON_SLICE,
                "<red><bold>Heal</bold></red>", "Restore your health safely.", "plexonutility.heal", "heal");
        addFeature(builder, player, 12, Feature.ENDERCHEST, Material.ENDER_CHEST,
                "<aqua><bold>Ender Chest</bold></aqua>", "Open your personal Ender Chest.", "plexonutility.enderchest", "enderchest");
        addAfk(builder, player, 13);
        addFeature(builder, player, 14, Feature.WORKBENCH, Material.CRAFTING_TABLE,
                "<gold><bold>Workbench</bold></gold>", "Open a portable crafting table.", "plexonutility.workbench", "workbench");
        addFeature(builder, player, 15, Feature.TRASH, Material.LAVA_BUCKET,
                "<red><bold>Trash</bold></red>", "Open a disposable inventory.", "plexonutility.trash", "trash");
        addHomes(builder, player, 16);

        builder.button(36, ecosystemIcon(), click -> openFamily(click.player()));
        builder.button(38, helpIcon(), click -> openHelp(click.player()));
        builder.button(40, refreshIcon(), click -> {
            family.refresh();
            open(click.player());
        });
        if (UtilityMenuModel.admin(player.hasPermission("plexonutility.admin")) == Availability.AVAILABLE) {
            builder.button(42, diagnosticsIcon(), click -> openDiagnostics(click.player()));
        }
        builder.button(44, closeIcon(), click -> click.player().closeInventory());

        builder.open(player);
    }

    private void openFamily(Player player) {
        List<FamilyStatus> statuses = family.snapshot();
        var builder = gui.builder("utility", "family-3.2", text.render(TextMode.MINIMESSAGE, FAMILY_TITLE), 5)
                .filler(Material.BLACK_STAINED_GLASS_PANE);
        addFrameAccents(builder);

        for (int index = 0; index < statuses.size() && index < FAMILY_SLOTS.length; index++) {
            FamilyStatus status = statuses.get(index);
            builder.button(FAMILY_SLOTS[index], familyStatusIcon(status), click -> { });
        }

        builder.button(36, backIcon(), click -> open(click.player()));
        builder.button(38, helpIcon(), click -> openHelp(click.player()));
        builder.button(40, refreshIcon(), click -> {
            family.refresh();
            openFamily(click.player());
        });
        builder.button(44, closeIcon(), click -> click.player().closeInventory());
        builder.open(player);
    }

    private void openHelp(Player player) {
        var builder = gui.builder("utility", "help-3.2", text.render(TextMode.MINIMESSAGE, HELP_TITLE), 5)
                .filler(Material.BLACK_STAINED_GLASS_PANE);
        addFrameAccents(builder);

        builder.button(11, icon(Material.LIME_DYE,
                "<green><bold>Availability</bold></green>",
                List.of(
                        "<green>AVAILABLE</green> <gray>• ready to use</gray>",
                        "<red>NO PERMISSION</red> <gray>• rank/permission required</gray>",
                        "<red>FEATURE DISABLED</red> <gray>• disabled by server config</gray>",
                        "<gray>INTEGRATION MISSING</gray> <gray>• provider is not active</gray>",
                        "<yellow>TEMPORARILY UNAVAILABLE</yellow> <gray>• provider is degraded</gray>",
                        "<red>ADMIN ONLY</red> <gray>• restricted diagnostics</gray>")), click -> { });

        builder.button(13, icon(Material.RED_BED,
                BRAND_OPEN + "Homes Ownership" + BRAND_CLOSE,
                List.of(
                        "<gray>PlexonUtility only navigates to Homes.</gray>",
                        "<gray>PlexonHomes owns persistence, limits,</gray>",
                        "<gray>safety rules and teleport behavior.</gray>",
                        "",
                        "<dark_gray>Rank limits use plexonhomes.limit.N / unlimited.</dark_gray>")), click -> { });

        builder.button(15, icon(Material.LAVA_BUCKET,
                "<red><bold>Trash Safety</bold></red>",
                List.of(
                        "<gray>The Trash inventory is intentionally writable.</gray>",
                        "<red>Items left inside are permanently destroyed.</red>",
                        "<dark_gray>This cannot be undone.</dark_gray>")), click -> { });

        builder.button(21, icon(Material.CLOCK,
                "<yellow><bold>Quiet Feedback</bold></yellow>",
                List.of(
                        "<gray>Routine successes use compact HUD feedback.</gray>",
                        "<gray>AFK uses its persistent bossbar.</gray>",
                        "<gray>Detailed errors and admin output may use chat.</gray>")), click -> { });

        builder.button(23, icon(Material.RECOVERY_COMPASS,
                BRAND_OPEN + "PlexonFamily" + BRAND_CLOSE,
                List.of(
                        "<gray>Integration state is lifecycle-driven.</gray>",
                        "<gray>No recurring plugin polling is performed.</gray>",
                        "<yellow>Use Refresh</yellow> <gray>for an explicit rescan.</gray>")), click -> openFamily(click.player()));

        builder.button(36, backIcon(), click -> open(click.player()));
        builder.button(40, refreshIcon(), click -> {
            family.refresh();
            openHelp(click.player());
        });
        builder.button(44, closeIcon(), click -> click.player().closeInventory());
        builder.open(player);
    }

    private void openDiagnostics(Player player) {
        if (UtilityMenuModel.admin(player.hasPermission("plexonutility.admin")) != Availability.AVAILABLE) {
            messages.send(player, "no-permission");
            return;
        }

        var builder = gui.builder("utility", "diagnostics-3.2", text.render(TextMode.MINIMESSAGE, ADMIN_TITLE), 5)
                .filler(Material.BLACK_STAINED_GLASS_PANE);
        addFrameAccents(builder);

        builder.button(11, icon(Material.COMPARATOR,
                "<green><bold>Core Services</bold></green>",
                List.of(
                        "<gray>Text:</gray> <green>READY</green>",
                        "<gray>GUI routing:</gray> <green>READY</green>",
                        "<gray>Scheduler bridge:</gray> <green>READY</green>",
                        "<gray>Integrations:</gray> <green>READY</green>",
                        "",
                        "<dark_gray>PlexonCore remains authoritative.</dark_gray>")), click -> { });

        builder.button(13, afkDiagnosticsIcon(player), click -> { });
        builder.button(15, familyDiagnosticsIcon(), click -> openFamily(click.player()));
        builder.button(22, homesDiagnosticsIcon(), click -> { });
        builder.button(24, icon(Material.WRITABLE_BOOK,
                "<red><bold>Command Diagnostics</bold></red>",
                List.of(
                        "<gray>/utilityadmin remains available for</gray>",
                        "<gray>console use and detailed text diagnostics.</gray>",
                        "<dark_gray>The GUI avoids routine chat dumping.</dark_gray>")), click -> { });

        builder.button(36, backIcon(), click -> open(click.player()));
        builder.button(40, refreshIcon(), click -> {
            family.refresh();
            openDiagnostics(click.player());
        });
        builder.button(44, closeIcon(), click -> click.player().closeInventory());
        builder.open(player);
    }

    private void addFeature(GuiService.GuiBuilder builder, Player viewer, int slot, Feature feature, Material material,
                            String name, String description, String permission, String command) {
        boolean enabled = config.get().enabled(feature);
        boolean permitted = viewer.hasPermission(permission);
        Availability availability = UtilityMenuModel.local(enabled, permitted);
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + description + "</gray>");
        if (feature == Feature.TRASH) {
            lore.add("<gray>Items left inside are permanently destroyed.</gray>");
            lore.add("<red><bold>Warning:</bold></red> <gray>This cannot be undone.</gray>");
        }
        lore.add("");
        lore.add(stateLine(availability));
        lore.add(permissionLine(permitted));
        lore.add("");
        lore.add(clickHint(availability, feature == Feature.TRASH ? "open Trash" : "use this utility"));

        builder.button(slot, icon(material, name, lore), click -> {
            Player player = click.player();
            Availability current = UtilityMenuModel.local(config.get().enabled(feature), player.hasPermission(permission));
            if (current == Availability.FEATURE_DISABLED) {
                messages.send(player, "feature-disabled");
                return;
            }
            if (current == Availability.NO_PERMISSION) {
                messages.send(player, "no-permission");
                return;
            }
            player.closeInventory();
            player.performCommand(command);
        });
    }

    private void addAfk(GuiService.GuiBuilder builder, Player viewer, int slot) {
        boolean enabled = config.get().enabled(Feature.AFK);
        boolean permitted = viewer.hasPermission("plexonutility.afk");
        boolean currentlyAfk = afk.isAfk(viewer.getUniqueId());
        Availability availability = UtilityMenuModel.local(enabled, permitted);
        List<String> lore = new ArrayList<>();
        lore.add(currentlyAfk
                ? "<gray>You are currently:</gray> <yellow>AFK</yellow>"
                : "<gray>You are currently:</gray> <green>ACTIVE</green>");
        lore.add(currentlyAfk
                ? "<gray>Your AFK bossbar is currently active.</gray>"
                : "<gray>Your AFK state is shared with TAB</gray>");
        lore.add("<gray>and other Plexon systems.</gray>");
        lore.add("");
        lore.add(stateLine(availability));
        lore.add(permissionLine(permitted));
        lore.add("");
        lore.add(clickHint(availability, currentlyAfk ? "return to active status" : "mark yourself AFK"));

        builder.button(slot, icon(Material.CLOCK, "<yellow><bold>AFK Status</bold></yellow>", lore), click -> {
            Player player = click.player();
            Availability current = UtilityMenuModel.local(config.get().enabled(Feature.AFK), player.hasPermission("plexonutility.afk"));
            if (current == Availability.FEATURE_DISABLED) {
                messages.send(player, "feature-disabled");
                return;
            }
            if (current == Availability.NO_PERMISSION) {
                messages.send(player, "no-permission");
                return;
            }
            player.performCommand("afk");
            open(player);
        });
    }

    private void addHomes(GuiService.GuiBuilder builder, Player viewer, int slot) {
        FamilyStatus homes = familyStatus("PLEXON_HOMES");
        boolean permitted = viewer.hasPermission("plexonhomes.gui");
        Availability availability = UtilityMenuModel.integration(homes.state(), permitted);
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Manage your saved homes.</gray>");
        lore.add("");
        lore.add("<gray>Provider:</gray> <white>PlexonHomes</white>");
        lore.add(stateLine(availability));
        lore.add(permissionLine(permitted));
        lore.add("<dark_gray>PlexonHomes owns home data and limits.</dark_gray>");
        lore.add("");
        lore.add(clickHint(availability, "open the Homes menu"));

        builder.button(slot, icon(Material.RED_BED, BRAND_OPEN + "Homes" + BRAND_CLOSE, lore), click -> {
            Player player = click.player();
            Availability current = homeAvailability(player);
            if (current == Availability.INTEGRATION_MISSING || current == Availability.TEMPORARILY_UNAVAILABLE) {
                family.refresh();
                current = homeAvailability(player);
            }
            if (current == Availability.NO_PERMISSION) {
                messages.send(player, "no-permission");
                return;
            }
            if (current == Availability.INTEGRATION_MISSING) {
                messages.sendRaw(player, "<red>PlexonHomes is not currently available.</red>");
                return;
            }
            if (current == Availability.TEMPORARILY_UNAVAILABLE) {
                messages.sendRaw(player, "<yellow>PlexonHomes is temporarily unavailable.</yellow>");
                return;
            }
            player.closeInventory();
            player.performCommand("homes");
        });
    }

    private Availability homeAvailability(Player player) {
        return UtilityMenuModel.integration(familyStatus("PLEXON_HOMES").state(), player.hasPermission("plexonhomes.gui"));
    }

    private FamilyStatus familyStatus(String integrationId) {
        return family.snapshot().stream()
                .filter(status -> status.id().equals(integrationId))
                .findFirst()
                .orElseGet(() -> new FamilyStatus(integrationId, integrationId, "-", IntegrationState.MISSING));
    }

    private ItemStack profileIcon(Player player, UtilityConfig cfg) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(text.render(TextMode.MINIMESSAGE,
                "<gradient:#57E389:#22D3EE><bold>Your Utility Profile</bold></gradient>"));
        boolean currentlyAfk = afk.isAfk(player.getUniqueId());
        meta.lore(List.of(
                text.renderTemplate("<gray>Player:</gray> <white><player></white>", Map.of("player", player.getName())),
                text.render(TextMode.MINIMESSAGE, "<gray>Status:</gray> <green>ONLINE</green>"),
                text.render(TextMode.MINIMESSAGE, currentlyAfk
                        ? "<gray>AFK:</gray> <yellow>AFK</yellow>"
                        : "<gray>AFK:</gray> <green>ACTIVE</green>"),
                text.renderTemplate("<gray>Available utilities:</gray> <white><available>/<total></white>", Map.of(
                        "available", availableUtilityCount(player, cfg), "total", TOTAL_PLAYER_UTILITIES)),
                text.render(TextMode.MINIMESSAGE, ""),
                text.render(TextMode.MINIMESSAGE, "<dark_gray>Your PlexonCraft utility controls.</dark_gray>"),
                text.render(TextMode.MINIMESSAGE, "<yellow>Click</yellow> <gray>to refresh live state.</gray>")));
        item.setItemMeta(meta);
        return item;
    }

    private int availableUtilityCount(Player player, UtilityConfig cfg) {
        int available = 0;
        if (UtilityMenuModel.local(cfg.enabled(Feature.FEED), player.hasPermission("plexonutility.feed")) == Availability.AVAILABLE) available++;
        if (UtilityMenuModel.local(cfg.enabled(Feature.HEAL), player.hasPermission("plexonutility.heal")) == Availability.AVAILABLE) available++;
        if (UtilityMenuModel.local(cfg.enabled(Feature.ENDERCHEST), player.hasPermission("plexonutility.enderchest")) == Availability.AVAILABLE) available++;
        if (UtilityMenuModel.local(cfg.enabled(Feature.AFK), player.hasPermission("plexonutility.afk")) == Availability.AVAILABLE) available++;
        if (UtilityMenuModel.local(cfg.enabled(Feature.WORKBENCH), player.hasPermission("plexonutility.workbench")) == Availability.AVAILABLE) available++;
        if (UtilityMenuModel.local(cfg.enabled(Feature.TRASH), player.hasPermission("plexonutility.trash")) == Availability.AVAILABLE) available++;
        if (homeAvailability(player) == Availability.AVAILABLE) available++;
        return available;
    }

    private ItemStack ecosystemIcon() {
        ItemStack item = new ItemStack(family.readyCount() > 0 ? Material.NETHER_STAR : Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text.render(TextMode.MINIMESSAGE, BRAND_OPEN + "PlexonFamily" + BRAND_CLOSE));
        meta.lore(List.of(
                text.renderTemplate("<gray>Connected services:</gray> <white><ready>/<total></white>", Map.of(
                        "ready", family.readyCount(), "total", family.totalCount())),
                text.render(TextMode.MINIMESSAGE, ""),
                text.render(TextMode.MINIMESSAGE, "<green>●</green> <gray>Ready integrations</gray>"),
                text.render(TextMode.MINIMESSAGE, "<dark_gray>●</dark_gray> <gray>Inactive integrations</gray>"),
                text.render(TextMode.MINIMESSAGE, ""),
                text.render(TextMode.MINIMESSAGE, "<yellow>Click</yellow> <gray>for integration details.</gray>")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack familyStatusIcon(FamilyStatus status) {
        Material material = switch (status.state()) {
            case READY -> Material.LIME_DYE;
            case MISSING -> Material.GRAY_DYE;
            case DEGRADED, INCOMPATIBLE -> Material.YELLOW_DYE;
            case FAILED -> Material.RED_DYE;
        };
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text.renderTemplate(BRAND_OPEN + "<plugin>" + BRAND_CLOSE, Map.of("plugin", status.pluginName())));
        meta.lore(List.of(
                text.renderTemplate("<gray>Version:</gray> <white><version></white>", Map.of("version", status.version())),
                text.renderTemplate(integrationStateLine(status.state()), Map.of("state", status.state().name())),
                text.render(TextMode.MINIMESSAGE, status.ready()
                        ? "<green>Service is ready.</green>"
                        : "<dark_gray>No player action is available here.</dark_gray>")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack familyDiagnosticsIcon() {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text.render(TextMode.MINIMESSAGE, BRAND_OPEN + "Family Health" + BRAND_CLOSE));
        meta.lore(List.of(
                text.renderTemplate("<gray>Ready:</gray> <white><ready>/<total></white>", Map.of(
                        "ready", family.readyCount(), "total", family.totalCount())),
                text.render(TextMode.MINIMESSAGE, "<gray>Refresh model:</gray> <green>LIFECYCLE / EXPLICIT</green>"),
                text.render(TextMode.MINIMESSAGE, "<gray>Polling:</gray> <green>NONE</green>"),
                text.render(TextMode.MINIMESSAGE, ""),
                text.render(TextMode.MINIMESSAGE, "<yellow>Click</yellow> <gray>for service details.</gray>")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack afkDiagnosticsIcon(Player player) {
        boolean enabled = config.get().enabled(Feature.AFK);
        boolean currentlyAfk = afk.isAfk(player.getUniqueId());
        return icon(Material.CLOCK,
                "<yellow><bold>AFK Runtime</bold></yellow>",
                List.of(
                        enabled ? "<gray>Feature:</gray> <green>ENABLED</green>" : "<gray>Feature:</gray> <red>DISABLED</red>",
                        currentlyAfk ? "<gray>Your state:</gray> <yellow>AFK</yellow>" : "<gray>Your state:</gray> <green>ACTIVE</green>",
                        "<gray>Feedback:</gray> <green>QUIET HUD MODEL</green>",
                        "<dark_gray>No GUI scheduler is running.</dark_gray>"));
    }

    private ItemStack homesDiagnosticsIcon() {
        FamilyStatus homes = familyStatus("PLEXON_HOMES");
        ItemStack item = new ItemStack(Material.RED_BED);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text.render(TextMode.MINIMESSAGE, BRAND_OPEN + "Homes Integration" + BRAND_CLOSE));
        meta.lore(List.of(
                text.renderTemplate(integrationStateLine(homes.state()), Map.of("state", homes.state().name())),
                text.renderTemplate("<gray>Version:</gray> <white><version></white>", Map.of("version", homes.version())),
                text.render(TextMode.MINIMESSAGE, "<gray>Authority:</gray> <white>PlexonHomes</white>"),
                text.render(TextMode.MINIMESSAGE, "<dark_gray>Utility does not cache or reinterpret home limits.</dark_gray>")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack helpIcon() {
        return icon(Material.BOOK,
                BRAND_OPEN + "Help / Information" + BRAND_CLOSE,
                List.of(
                        "<gray>Understand permissions, integrations</gray>",
                        "<gray>and Utility ownership boundaries.</gray>",
                        "<yellow>Click</yellow> <gray>to open help.</gray>"));
    }

    private ItemStack refreshIcon() {
        return icon(Material.SUNFLOWER,
                BRAND_OPEN + "Refresh" + BRAND_CLOSE,
                List.of(
                        "<gray>Rerender current live state.</gray>",
                        "<gray>Integration refresh is explicit, not polled.</gray>",
                        "<yellow>Click</yellow> <gray>to refresh.</gray>"));
    }

    private ItemStack diagnosticsIcon() {
        return icon(Material.COMPARATOR,
                "<red><bold>Admin Diagnostics</bold></red>",
                List.of(
                        "<gray>Inspect Utility runtime health.</gray>",
                        "<gray>Core:</gray> <green>READY</green>",
                        "<gray>Family:</gray> <white>LIVE SNAPSHOT</white>",
                        "<yellow>Click</yellow> <gray>for diagnostics.</gray>"));
    }

    private ItemStack backIcon() {
        return icon(Material.ARROW,
                BRAND_OPEN + "Back" + BRAND_CLOSE,
                List.of("<yellow>Click</yellow> <gray>to return to the Utility hub.</gray>"));
    }

    private ItemStack closeIcon() {
        return icon(Material.BARRIER,
                "<red><bold>Close</bold></red>",
                List.of("<yellow>Click</yellow> <gray>to close this menu.</gray>"));
    }

    private void addFrameAccents(GuiService.GuiBuilder builder) {
        int[] green = {0, 9, 18, 27};
        int[] cyan = {8, 17, 26, 35};
        for (int slot : green) decorative(builder, slot, Material.LIME_STAINED_GLASS_PANE);
        for (int slot : cyan) decorative(builder, slot, Material.CYAN_STAINED_GLASS_PANE);
    }

    private void decorative(GuiService.GuiBuilder builder, int slot, Material material) {
        builder.button(slot, icon(material, " ", List.of()), click -> { });
    }

    private String stateLine(Availability availability) {
        return switch (availability) {
            case AVAILABLE -> "<gray>Status:</gray> <green>AVAILABLE</green>";
            case NO_PERMISSION -> "<gray>Status:</gray> <red>NO PERMISSION</red>";
            case FEATURE_DISABLED -> "<gray>Status:</gray> <red>FEATURE DISABLED</red>";
            case INTEGRATION_MISSING -> "<gray>Status:</gray> <gray>INTEGRATION MISSING</gray>";
            case TEMPORARILY_UNAVAILABLE -> "<gray>Status:</gray> <yellow>TEMPORARILY UNAVAILABLE</yellow>";
            case ADMIN_ONLY -> "<gray>Status:</gray> <red>ADMIN ONLY</red>";
        };
    }

    private String permissionLine(boolean permitted) {
        return permitted
                ? "<gray>Permission:</gray> <green>GRANTED</green>"
                : "<gray>Permission:</gray> <red>NOT GRANTED</red>";
    }

    private String clickHint(Availability availability, String action) {
        return availability == Availability.AVAILABLE
                ? "<yellow>Click</yellow> <gray>to " + action + ".</gray>"
                : "<dark_gray>Action unavailable in the current state.</dark_gray>";
    }

    private String integrationStateLine(IntegrationState state) {
        return switch (state) {
            case READY -> "<gray>Status:</gray> <green><state></green>";
            case MISSING -> "<gray>Status:</gray> <gray><state></gray>";
            case DEGRADED, INCOMPATIBLE -> "<gray>Status:</gray> <yellow><state></yellow>";
            case FAILED -> "<gray>Status:</gray> <red><state></red>";
        };
    }

    private ItemStack icon(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(text.render(TextMode.MINIMESSAGE, name));
        List<Component> lines = lore.stream().map(line -> text.render(TextMode.MINIMESSAGE, line)).toList();
        meta.lore(lines);
        item.setItemMeta(meta);
        return item;
    }
}
