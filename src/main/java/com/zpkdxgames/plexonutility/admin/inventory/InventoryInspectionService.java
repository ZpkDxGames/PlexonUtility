package com.zpkdxgames.plexonutility.admin.inventory;

import com.zpkdxgames.plexoncore.gui.GuiService;
import com.zpkdxgames.plexonutility.menu.UtilityMenuItemFactory;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Safe online inventory inspection/editor.
 *
 * <p>View access is controlled by {@code plexonutility.admin.invsee}; live mutation is separately
 * gated by {@code plexonutility.admin.invsee.edit}. Edits are applied immediately to the target's
 * live PlayerInventory one slot at a time. There is deliberately no close-time bulk commit.</p>
 */
public final class InventoryInspectionService implements Listener {
    static final String EDIT_PERMISSION = "plexonutility.admin.invsee.edit";
    static final int VIEW_SIZE = 54;
    static final int REFRESH_SLOT = 52;
    static final int INFO_SLOT = 53;

    private final UtilityMenuItemFactory items;
    private final MessageService messages;
    private final Map<UUID, OpenView> openViews = new ConcurrentHashMap<>();

    public InventoryInspectionService(GuiService gui, UtilityMenuItemFactory items, MessageService messages) {
        Objects.requireNonNull(gui, "gui"); // Keep the Core GUI lifecycle boundary explicit for Utility admin surfaces.
        this.items = Objects.requireNonNull(items, "items");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public boolean open(Player viewer, Player target) {
        if (target == null || !target.isOnline() || !target.isConnected()) {
            messages.send(viewer, "admin-target-offline");
            return false;
        }

        boolean editable = viewer.hasPermission(EDIT_PERMISSION);
        Inventory view = viewer.getServer().createInventory(
                null,
                VIEW_SIZE,
                items.render(editable
                        ? "<aqua><bold>Inventory Editor</bold></aqua>"
                        : "<aqua><bold>Inventory Inspect</bold></aqua>"));
        renderFrame(view);
        renderSnapshot(view, target, snapshot(target), editable);

        // openInventory may close a previous view and trigger onClose, so store the new session afterwards.
        viewer.openInventory(view);
        openViews.put(viewer.getUniqueId(), new OpenView(target.getUniqueId(), view, editable));
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        OpenView session = matchingSession(viewer, event.getView().getTopInventory());
        if (session == null) return;

        int rawSlot = event.getRawSlot();
        if (rawSlot >= 0 && rawSlot < VIEW_SIZE) {
            event.setCancelled(true);
            Player target = onlineTarget(viewer, session);
            if (target == null) {
                closeOfflineTarget(viewer);
                return;
            }

            if (rawSlot == REFRESH_SLOT) {
                refresh(session, target);
                return;
            }

            SlotRef ref = slotForRaw(rawSlot);
            if (ref == null || !session.editable() || !viewer.hasPermission(EDIT_PERMISSION)) return;

            ClickType click = event.getClick();
            if (click != ClickType.LEFT && click != ClickType.RIGHT) return;

            ItemStack before = readTarget(target, ref);
            ItemStack cursor = cloneNullable(viewer.getItemOnCursor());
            EditResult result = click == ClickType.LEFT
                    ? leftEdit(before, cursor)
                    : rightEdit(before, cursor);
            if (!result.changed()) return;

            writeTarget(target, ref, result.slot());
            viewer.setItemOnCursor(cloneNullable(result.cursor()));
            target.updateInventory();
            refreshTargetViews(target);
            auditEdit(viewer, target, ref, before, result.slot());
            return;
        }

        // Never let vanilla transfer semantics route items into the mirrored top inventory.
        if (event.isShiftClick() || event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player viewer)) return;
        OpenView session = matchingSession(viewer, event.getView().getTopInventory());
        if (session == null) return;
        if (event.getRawSlots().stream().anyMatch(slot -> slot >= 0 && slot < VIEW_SIZE)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID leaving = event.getPlayer().getUniqueId();
        openViews.remove(leaving);
        for (var entry : List.copyOf(openViews.entrySet())) {
            if (!entry.getValue().targetId().equals(leaving)) continue;
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
        UUID viewerId = event.getPlayer().getUniqueId();
        OpenView session = openViews.get(viewerId);
        if (session != null && session.inventory().equals(event.getView().getTopInventory())) {
            openViews.remove(viewerId);
        }
    }

    private void renderFrame(Inventory view) {
        ItemStack filler = items.icon(Material.BLACK_STAINED_GLASS_PANE, "<dark_gray> </dark_gray>", List.of());
        for (int slot = 0; slot < VIEW_SIZE; slot++) view.setItem(slot, filler.clone());
    }

    private void renderSnapshot(Inventory view, Player target, Snapshot snapshot, boolean editable) {
        for (int slot = 0; slot < 36; slot++) view.setItem(slot, cloneNullable(snapshot.storage().get(slot)));
        view.setItem(45, cloneNullable(snapshot.boots()));
        view.setItem(46, cloneNullable(snapshot.leggings()));
        view.setItem(47, cloneNullable(snapshot.chestplate()));
        view.setItem(48, cloneNullable(snapshot.helmet()));
        view.setItem(50, cloneNullable(snapshot.offhand()));
        view.setItem(REFRESH_SLOT, items.icon(Material.CLOCK, "<yellow><bold>Refresh</bold></yellow>", List.of(
                "<gray>Reload the displayed slots from the live target.</gray>",
                "<dark_gray>Useful when the target moves items themselves.</dark_gray>")));
        view.setItem(INFO_SLOT, items.head(target,
                items.template("<aqua><bold><player></bold></aqua>", Map.of("player", snapshot.name())),
                editable
                        ? List.of(
                                items.template("<gray>UUID:</gray> <white><uuid></white>", Map.of("uuid", snapshot.targetId())),
                                items.render("<gray>Mode:</gray> <green>EDITABLE</green>"),
                                items.render(""),
                                items.render("<yellow>Left-click</yellow> <gray>swaps a full stack.</gray>"),
                                items.render("<yellow>Right-click</yellow> <gray>splits or places one item.</gray>"),
                                items.render("<dark_gray>Shift-transfer, double-click and target-pane drag are blocked.</dark_gray>"),
                                items.render("<dark_gray>Edits write immediately; closing never performs a bulk save.</dark_gray>"))
                        : List.of(
                                items.template("<gray>UUID:</gray> <white><uuid></white>", Map.of("uuid", snapshot.targetId())),
                                items.render("<gray>Mode:</gray> <green>READ ONLY</green>"),
                                items.render(""),
                                items.render("<dark_gray>Grant plexonutility.admin.invsee.edit for live editing.</dark_gray>"),
                                items.render("<dark_gray>Armor: slots 45–48 • Offhand: slot 50</dark_gray>"))));
    }

    private OpenView matchingSession(Player viewer, Inventory top) {
        OpenView session = openViews.get(viewer.getUniqueId());
        if (session == null) return null;
        if (!session.inventory().equals(top)) {
            openViews.remove(viewer.getUniqueId());
            return null;
        }
        return session;
    }

    private Player onlineTarget(Player viewer, OpenView session) {
        Player target = viewer.getServer().getPlayer(session.targetId());
        return target != null && target.isOnline() && target.isConnected() ? target : null;
    }

    private void closeOfflineTarget(Player viewer) {
        openViews.remove(viewer.getUniqueId());
        viewer.closeInventory();
        messages.send(viewer, "admin-target-offline");
    }

    private void refresh(OpenView session, Player target) {
        renderSnapshot(session.inventory(), target, snapshot(target), session.editable());
    }

    private void refreshTargetViews(Player target) {
        UUID targetId = target.getUniqueId();
        for (var entry : List.copyOf(openViews.entrySet())) {
            OpenView session = entry.getValue();
            if (!session.targetId().equals(targetId)) continue;
            Player viewer = target.getServer().getPlayer(entry.getKey());
            if (viewer == null || !viewer.isOnline()
                    || !viewer.getOpenInventory().getTopInventory().equals(session.inventory())) {
                openViews.remove(entry.getKey());
                continue;
            }
            refresh(session, target);
        }
    }

    static EditResult leftEdit(ItemStack current, ItemStack cursor) {
        if (sameStack(current, cursor)) return new EditResult(cloneNullable(current), cloneNullable(cursor), false);
        return new EditResult(cloneNullable(cursor), cloneNullable(current), true);
    }

    static EditResult rightEdit(ItemStack current, ItemStack cursor) {
        if (empty(cursor)) {
            if (empty(current)) return new EditResult(null, null, false);
            int take = (current.getAmount() + 1) / 2;
            int remain = current.getAmount() - take;
            return new EditResult(copyWithAmount(current, remain), copyWithAmount(current, take), true);
        }

        if (empty(current)) {
            return new EditResult(copyWithAmount(cursor, 1), copyWithAmount(cursor, cursor.getAmount() - 1), true);
        }

        if (current.isSimilar(cursor) && current.getAmount() < current.getMaxStackSize()) {
            return new EditResult(
                    copyWithAmount(current, current.getAmount() + 1),
                    copyWithAmount(cursor, cursor.getAmount() - 1),
                    true);
        }

        return leftEdit(current, cursor);
    }

    private static SlotRef slotForRaw(int rawSlot) {
        if (rawSlot >= 0 && rawSlot < 36) return new SlotRef(SlotKind.STORAGE, rawSlot, "storage." + rawSlot);
        return switch (rawSlot) {
            case 45 -> new SlotRef(SlotKind.BOOTS, -1, "boots");
            case 46 -> new SlotRef(SlotKind.LEGGINGS, -1, "leggings");
            case 47 -> new SlotRef(SlotKind.CHESTPLATE, -1, "chestplate");
            case 48 -> new SlotRef(SlotKind.HELMET, -1, "helmet");
            case 50 -> new SlotRef(SlotKind.OFFHAND, -1, "offhand");
            default -> null;
        };
    }

    private static ItemStack readTarget(Player target, SlotRef ref) {
        PlayerInventory inventory = target.getInventory();
        ItemStack item = switch (ref.kind()) {
            case STORAGE -> inventory.getItem(ref.index());
            case BOOTS -> inventory.getBoots();
            case LEGGINGS -> inventory.getLeggings();
            case CHESTPLATE -> inventory.getChestplate();
            case HELMET -> inventory.getHelmet();
            case OFFHAND -> inventory.getItemInOffHand();
        };
        return cloneNullable(item);
    }

    private static void writeTarget(Player target, SlotRef ref, ItemStack item) {
        PlayerInventory inventory = target.getInventory();
        ItemStack copy = cloneNullable(item);
        switch (ref.kind()) {
            case STORAGE -> inventory.setItem(ref.index(), copy);
            case BOOTS -> inventory.setBoots(copy);
            case LEGGINGS -> inventory.setLeggings(copy);
            case CHESTPLATE -> inventory.setChestplate(copy);
            case HELMET -> inventory.setHelmet(copy);
            case OFFHAND -> inventory.setItemInOffHand(copy == null ? new ItemStack(Material.AIR) : copy);
        }
    }

    private static void auditEdit(Player viewer, Player target, SlotRef ref, ItemStack before, ItemStack after) {
        viewer.getServer().getLogger().info("ADMIN action=invsee-edit"
                + " actor=" + viewer.getName()
                + " actor_uuid=" + viewer.getUniqueId()
                + " target=" + target.getName()
                + " target_uuid=" + target.getUniqueId()
                + " slot=" + ref.auditName()
                + " before=" + stackSummary(before)
                + " after=" + stackSummary(after));
    }

    private static String stackSummary(ItemStack item) {
        if (empty(item)) return "empty";
        return item.getType().name().toLowerCase(Locale.ROOT) + "x" + item.getAmount();
    }

    private static boolean sameStack(ItemStack first, ItemStack second) {
        if (empty(first) && empty(second)) return true;
        if (empty(first) || empty(second)) return false;
        return first.getAmount() == second.getAmount() && first.isSimilar(second);
    }

    private static boolean empty(ItemStack item) {
        if (item == null || item.getAmount() <= 0) return true;
        Material type = item.getType();
        return type == Material.AIR || type == Material.CAVE_AIR || type == Material.VOID_AIR;
    }

    private static ItemStack copyWithAmount(ItemStack item, int amount) {
        if (empty(item) || amount <= 0) return null;
        ItemStack copy = item.clone();
        copy.setAmount(amount);
        return copy;
    }

    private static ItemStack cloneNullable(ItemStack item) {
        return empty(item) ? null : item.clone();
    }

    private static ItemStack cloneSafe(ItemStack item) {
        return item == null ? new ItemStack(Material.AIR) : item.clone();
    }

    static record EditResult(ItemStack slot, ItemStack cursor, boolean changed) { }

    private enum SlotKind {
        STORAGE,
        BOOTS,
        LEGGINGS,
        CHESTPLATE,
        HELMET,
        OFFHAND
    }

    private record SlotRef(SlotKind kind, int index, String auditName) { }

    private record OpenView(UUID targetId, Inventory inventory, boolean editable) { }

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
