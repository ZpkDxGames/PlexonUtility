package com.zpkdxgames.plexonutility.menu;

import com.zpkdxgames.plexoncore.text.TextService;
import com.zpkdxgames.plexoncore.text.TextService.TextMode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Small presentation helper shared by the player hub and admin menus. */
public final class UtilityMenuItemFactory {
    private final TextService text;

    public UtilityMenuItemFactory(TextService text) {
        this.text = Objects.requireNonNull(text, "text");
    }

    public ItemStack icon(Material material, String trustedName, List<String> trustedLore) {
        return item(material, render(trustedName), trustedLore.stream().map(this::render).toList());
    }

    public ItemStack item(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        apply(meta, name, lore);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack head(OfflinePlayer owner, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(owner);
        apply(meta, name, lore);
        item.setItemMeta(meta);
        return item;
    }

    public Component render(String trustedMiniMessage) {
        return plainStyle(text.render(TextMode.MINIMESSAGE, trustedMiniMessage));
    }

    public Component template(String trustedTemplate, Map<String, ?> values) {
        return plainStyle(text.renderTemplate(trustedTemplate, values));
    }

    public Component plainStyle(Component component) {
        return component.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    private void apply(ItemMeta meta, Component name, List<Component> lore) {
        meta.displayName(plainStyle(name));
        meta.lore(lore.stream().map(this::plainStyle).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
    }
}
