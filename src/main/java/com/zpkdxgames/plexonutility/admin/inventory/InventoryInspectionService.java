package com.zpkdxgames.plexonutility.admin.inventory;

import com.zpkdxgames.plexoncore.gui.GuiService;
import com.zpkdxgames.plexonutility.menu.UtilityMenuItemFactory;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Read-only snapshot inspector. Every displayed target item is cloned. */
public final class InventoryInspectionService implements Listener {
    private final GuiService gui;
    private final UtilityMenuItemFactory items;
    private final MessageService messages;
    private final Map<UUID, UUID> openViews = new ConcurrentHashMap<>();

    public InventoryInspectionService(GuiService gui, UtilityMenuItemFactory items, MessageService messages) {
        this.gui = Objects.requireNonNull(gui, "gui");
        this.items = Objects.requireNonNull(items, "items");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public boolean open(Player viewer, Player target) {
        if (target == null || !target.isOnline() || !target.isConnected()) {
            messages.send(viewer, "admin-target-offline");
            return false;
        }
        Snapshot snapshot = snapshot(target);
        var builder = gui.builder("utility", "invsee-3.3", items.render("<aqua><bold>Inventory Inspect</bold></aqua>"), 6)
                .filler(Material.BLACK_STAINED_GLASS_PANE);

        for (int slot = 0; slot < snapshot.storage().size() && slot < 36; slot++) {
            builder.button(slot, snapshot.storage().get(slot), click -> { });
        }
        builder.button(45, snapshot.boots(), click -> { });
        builder.button(46, snapshot.leggings(), click -> { });
        builder.button(47, snapshot.chestplate(), click -> { });
        builder.button(48, snapshot.helmet(), click -> { });
        builder.button(50, snapshot.offhand(), click -> { });
        builder.button(53, items.head(target,
                items.template("<aqua><bold><player></bold></aqua>", Map.of("player", snapshot.name())),
                List.of(
                        items.template("<gray>UUID:</gray> <white><uuid></white>", Map.of("uuid", snapshot.targetId())),
                        items.render("<gray>Mode:</gray> <green>READ ONLY</green>"),
                        items.render(""),
                        items.render("<dark_gray>Armor: slots 45–48 • Offhand: slot 50</dark_gray>"),
                        items.render("<dark_gray>Closing this view never writes to the target.</dark_gray>"))), click -> { });
        builder.open(viewer);
        openViews.put(viewer.getUniqueId(), target.getUniqueId());
        return true;
    }

    public Snapshot snapshot(Player target) {
        PlayerInventory inventory = target.getInventory();
        List<ItemStack> storage = new ArrayList<>(36);
        for (ItemStack item : inventory.getStorageContents()) storage.add(cloneSafe(item));
        while (storage.size() < 36) storage.add(new ItemStack(Material.AIR));
        return new Snapshot(
                target.getUniqueId(),
                target.getName(),
                storage,
                cloneSafe(inventory.getHelmet()),
                cloneSafe(inventory.getChestplate()),
                cloneSafe(inventory.getLeggings()),
                cloneSafe(inventory.getBoots()),
                cloneSafe(inventory.getItemInOffHand()));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID leaving = event.getPlayer().getUniqueId();
        openViews.remove(leaving);
        for (var entry : List.copyOf(openViews.entrySet())) {
            if (!entry.getValue().equals(leaving)) continue;
            Player viewer = event.getPlayer().getServer().getPlayer(entry.getKey());
            openViews.remove(entry.getKey());
            if (viewer != null && viewer.isOnline()) {
                viewer.closeInventory();
                messages.send(viewer, "admin-target-offline");
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        openViews.remove(event.getPlayer().getUniqueId());
    }

    private static ItemStack cloneSafe(ItemStack item) {
        return item == null ? new ItemStack(Material.AIR) : item.clone();
    }

    public record Snapshot(
            UUID targetId,
            String name,
            List<ItemStack> storage,
            ItemStack helmet,
            ItemStack chestplate,
            ItemStack leggings,
            ItemStack boots,
            ItemStack offhand) {
        public Snapshot {
            storage = storage.stream().map(InventoryInspectionService::cloneSafe).toList();
            helmet = cloneSafe(helmet);
            chestplate = cloneSafe(chestplate);
            leggings = cloneSafe(leggings);
            boots = cloneSafe(boots);
            offhand = cloneSafe(offhand);
        }
    }
}
