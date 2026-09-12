package com.zpkdxgames.plexonutility.menu;

import com.zpkdxgames.plexoncore.gui.GuiService;
import com.zpkdxgames.plexoncore.text.TextService;
import com.zpkdxgames.plexoncore.text.TextService.TextMode;
import com.zpkdxgames.plexonutility.afk.AfkManager;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.integration.FamilyCompatibilityService;
import com.zpkdxgames.plexonutility.message.MessageService;
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

public final class UtilityMenuService implements CommandExecutor {
    private static final String TITLE = "<gradient:#57E389:#22D3EE><bold>PlexonUtility</bold></gradient> <dark_gray>•</dark_gray> <gray>Utility Hub</gray>";
    private static final String BRAND_OPEN = "<gradient:#57E389:#22D3EE><bold>";
    private static final String BRAND_CLOSE = "</bold></gradient>";

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
        var builder = gui.builder("utility", "hub-v3", text.render(TextMode.MINIMESSAGE, TITLE), 4)
                .filler(Material.BLACK_STAINED_GLASS_PANE);

        builder.button(4, profileIcon(player, cfg), click -> open(click.player()));

        addFeature(builder, player, 10, Feature.FEED, Material.GOLDEN_CARROT,
                "Feed", "Restore hunger and saturation.", "plexonutility.feed", "feed");
        addFeature(builder, player, 11, Feature.HEAL, Material.GLISTERING_MELON_SLICE,
                "Heal", "Restore health and clear configured effects.", "plexonutility.heal", "heal");
        addFeature(builder, player, 12, Feature.ENDERCHEST, Material.ENDER_CHEST,
                "Ender Chest", "Open your Ender Chest anywhere.", "plexonutility.enderchest", "enderchest");
        addAfk(builder, player, 13);
        addFeature(builder, player, 14, Feature.WORKBENCH, Material.CRAFTING_TABLE,
                "Workbench", "Open a virtual crafting table.", "plexonutility.workbench", "workbench");
        addFeature(builder, player, 15, Feature.TRASH, Material.HOPPER,
                "Trash", "Open a disposable 27-slot trash inventory.", "plexonutility.trash", "trash");
        addHomes(builder, player, 16);

        builder.button(22, ecosystemIcon(), click -> {
            family.refresh();
            open(click.player());
        });

        builder.button(27, icon(Material.SUNFLOWER,
                BRAND_OPEN + "Refresh" + BRAND_CLOSE,
                List.of("<gray>Refresh live integration status.</gray>", "<dark_gray>Click to redraw this menu.</dark_gray>")),
                click -> {
                    family.refresh();
                    open(click.player());
                });

        builder.button(31, icon(Material.BARRIER,
                "<red><bold>Close</bold></red>",
                List.of("<gray>Close the utility hub.</gray>")), click -> click.player().closeInventory());

        if (player.hasPermission("plexonutility.admin")) {
            builder.button(35, icon(Material.COMPARATOR,
                    BRAND_OPEN + "Diagnostics" + BRAND_CLOSE,
                    List.of("<gray>Inspect Utility, Core and integration health.</gray>",
                            "<dark_gray>Click to print diagnostics.</dark_gray>")), click -> {
                click.player().closeInventory();
                click.player().performCommand("utilityadmin diagnostics");
            });
        }

        builder.open(player);
    }

    private void addFeature(GuiService.GuiBuilder builder, Player viewer, int slot, Feature feature, Material material,
                            String name, String description, String permission, String command) {
        boolean enabled = config.get().enabled(feature);
        boolean permitted = viewer.hasPermission(permission);
        Material shownMaterial = enabled && permitted ? material : Material.BARRIER;
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + description + "</gray>");
        if (!enabled) {
            lore.add("<red>Disabled by server configuration.</red>");
        } else if (!permitted) {
            lore.add("<red>Permission required:</red> <dark_gray>" + permission + "</dark_gray>");
        } else {
            lore.add("<green>Available</green> <dark_gray>• Click to use</dark_gray>");
        }
        builder.button(slot, icon(shownMaterial, BRAND_OPEN + name + BRAND_CLOSE, lore), click -> {
            Player player = click.player();
            if (!config.get().enabled(feature)) {
                messages.send(player, "feature-disabled");
                return;
            }
            if (!player.hasPermission(permission)) {
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
        Material material = enabled && permitted ? (currentlyAfk ? Material.REDSTONE_TORCH : Material.CLOCK) : Material.BARRIER;
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Toggle your current AFK state.</gray>");
        lore.add("<gray>Current:</gray> " + (currentlyAfk ? "<yellow>AFK</yellow>" : "<green>Active</green>"));
        if (!enabled) lore.add("<red>Disabled by server configuration.</red>");
        else if (!permitted) lore.add("<red>Permission required:</red> <dark_gray>plexonutility.afk</dark_gray>");
        else lore.add("<dark_gray>Click to toggle and refresh.</dark_gray>");

        builder.button(slot, icon(material, BRAND_OPEN + "AFK" + BRAND_CLOSE, lore), click -> {
            Player player = click.player();
            if (!config.get().enabled(Feature.AFK)) {
                messages.send(player, "feature-disabled");
                return;
            }
            if (!player.hasPermission("plexonutility.afk")) {
                messages.send(player, "no-permission");
                return;
            }
            player.performCommand("afk");
            open(player);
        });
    }

    private void addHomes(GuiService.GuiBuilder builder, Player viewer, int slot) {
        boolean available = family.ready("PLEXON_HOMES");
        boolean permitted = viewer.hasPermission("plexonhomes.gui");
        Material material = available && permitted ? Material.RED_BED : Material.BARRIER;
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Open your PlexonHomes browser.</gray>");
        lore.add("<dark_gray>PlexonHomes remains the authoritative home service.</dark_gray>");
        if (!available) lore.add("<yellow>PlexonHomes is not currently available.</yellow>");
        else if (!permitted) lore.add("<red>Permission required:</red> <dark_gray>plexonhomes.gui</dark_gray>");
        else lore.add("<green>Integrated</green> <dark_gray>• Click to open /homes</dark_gray>");

        builder.button(slot, icon(material, BRAND_OPEN + "Homes" + BRAND_CLOSE, lore), click -> {
            Player player = click.player();
            if (!family.ready("PLEXON_HOMES")) {
                family.refresh();
                if (!family.ready("PLEXON_HOMES")) {
                    messages.sendRaw(player, "<yellow>PlexonHomes is not currently available.</yellow>");
                    return;
                }
            }
            if (!player.hasPermission("plexonhomes.gui")) {
                messages.send(player, "no-permission");
                return;
            }
            player.closeInventory();
            player.performCommand("homes");
        });
    }

    private ItemStack profileIcon(Player player, UtilityConfig cfg) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(player);
        meta.displayName(text.renderTemplate(BRAND_OPEN + "<player>" + BRAND_CLOSE, Map.of("player", player.getName())));
        String afkState = afk.isAfk(player.getUniqueId()) ? "AFK" : "Active";
        meta.lore(List.of(
                text.renderTemplate("<gray>Status:</gray> <white><state></white>", Map.of("state", afkState)),
                text.renderTemplate("<gray>Utilities enabled:</gray> <white><count></white>", Map.of("count", cfg.enabledFeatures().size())),
                text.renderTemplate("<gray>PlexonFamily ready:</gray> <white><ready>/<total></white>", Map.of(
                        "ready", family.readyCount(), "total", family.totalCount())),
                text.render(TextMode.MINIMESSAGE, "<dark_gray>Click to refresh this view.</dark_gray>")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack ecosystemIcon() {
        int ready = family.readyCount();
        int total = family.totalCount();
        Material material = ready > 0 ? Material.RECOVERY_COMPASS : Material.COMPASS;
        return icon(material, BRAND_OPEN + "PlexonFamily" + BRAND_CLOSE,
                List.of(
                        "<gray>Shared Core integration registry status.</gray>",
                        "<gray>Ready:</gray> <white>" + ready + "/" + total + "</white>",
                        family.ready("PLEXON_HOMES") ? "<green>Homes integration ready.</green>" : "<dark_gray>Homes integration not detected.</dark_gray>",
                        "<dark_gray>Click to refresh integration state.</dark_gray>"));
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
