package com.zpkdxgames.plexonutility.menu;

import com.zpkdxgames.plexoncore.gui.GuiService;
import com.zpkdxgames.plexoncore.text.TextService;
import com.zpkdxgames.plexoncore.text.TextService.TextMode;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import com.zpkdxgames.plexonutility.feature.Feature;
import com.zpkdxgames.plexonutility.message.MessageService;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class UtilityMenuService implements CommandExecutor {
    private static final String TITLE = "<gradient:#57E389:#22D3EE><bold>PlexonUtility</bold></gradient> <dark_gray>•</dark_gray> <gray>Utilities</gray>";

    private final Supplier<UtilityConfig> config;
    private final MessageService messages;
    private final GuiService gui;
    private final TextService text;

    public UtilityMenuService(Supplier<UtilityConfig> config, MessageService messages, GuiService gui, TextService text) {
        this.config = config;
        this.messages = messages;
        this.gui = gui;
        this.text = text;
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
        var builder = gui.builder("utility", "hub", text.render(TextMode.MINIMESSAGE, TITLE), 3)
                .filler(Material.GRAY_STAINED_GLASS_PANE);

        addFeature(builder, 10, Feature.FEED, Material.GOLDEN_CARROT, "Feed", "Restore hunger and saturation.", "plexonutility.feed", "feed");
        addFeature(builder, 11, Feature.HEAL, Material.GLISTERING_MELON_SLICE, "Heal", "Restore health and clear configured effects.", "plexonutility.heal", "heal");
        addFeature(builder, 12, Feature.ENDERCHEST, Material.ENDER_CHEST, "Ender Chest", "Open your Ender Chest anywhere.", "plexonutility.enderchest", "enderchest");
        addFeature(builder, 13, Feature.WORKBENCH, Material.CRAFTING_TABLE, "Workbench", "Open a virtual crafting table.", "plexonutility.workbench", "workbench");
        addFeature(builder, 14, Feature.AFK, Material.CLOCK, "AFK", "Toggle your current AFK state.", "plexonutility.afk", "afk");
        addFeature(builder, 15, Feature.TRASH, Material.HOPPER, "Trash", "Open a disposable 27-slot trash inventory.", "plexonutility.trash", "trash");

        if (player.hasPermission("plexonutility.admin")) {
            builder.button(22, icon(Material.COMPARATOR,
                    "<gradient:#57E389:#22D3EE><bold>Diagnostics</bold></gradient>",
                    List.of("<gray>Inspect Utility, Core and complement health.</gray>", "<dark_gray>Click to view diagnostics.</dark_gray>")), click -> {
                click.player().closeInventory();
                click.player().performCommand("utilityadmin diagnostics");
            });
        }
        builder.open(player);
    }

    private void addFeature(GuiService.GuiBuilder builder, int slot, Feature feature, Material material, String name,
                            String description, String permission, String command) {
        boolean enabled = config.get().enabled(feature);
        Material shownMaterial = enabled ? material : Material.BARRIER;
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + description + "</gray>");
        lore.add(enabled ? "<green>Enabled</green> <dark_gray>• Click to use</dark_gray>" : "<red>Disabled by server configuration.</red>");
        builder.button(slot, icon(shownMaterial,
                "<gradient:#57E389:#22D3EE><bold>" + name + "</bold></gradient>", lore), click -> {
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
