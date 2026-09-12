package com.zpkdxgames.plexonutility.admin.inventory;

import com.zpkdxgames.plexoncore.gui.GuiService;
import com.zpkdxgames.plexonutility.menu.UtilityMenuItemFactory;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryInspectionServiceTest {
    @Test void snapshotClonesStorageArmorAndOffhand() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        UUID id = UUID.randomUUID();
        CloneChain storage = cloneChain(3);
        CloneChain helmet = cloneChain(1);
        CloneChain chestplate = cloneChain(1);
        CloneChain leggings = cloneChain(1);
        CloneChain boots = cloneChain(1);
        CloneChain offhand = cloneChain(1);
        ItemStack[] storageContents = new ItemStack[36];
        Arrays.fill(storageContents, storage.original());

        when(player.getUniqueId()).thenReturn(id);
        when(player.getName()).thenReturn("Target");
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getStorageContents()).thenReturn(storageContents);
        when(inventory.getHelmet()).thenReturn(helmet.original());
        when(inventory.getChestplate()).thenReturn(chestplate.original());
        when(inventory.getLeggings()).thenReturn(leggings.original());
        when(inventory.getBoots()).thenReturn(boots.original());
        when(inventory.getItemInOffHand()).thenReturn(offhand.original());

        InventoryInspectionService.Snapshot snapshot = service().snapshot(player);

        assertEquals(id, snapshot.targetId());
        assertEquals(36, snapshot.storage().size());
        assertEquals(3, snapshot.storage().getFirst().getAmount());
        assertNotSame(storage.original(), snapshot.storage().getFirst());
        assertNotSame(helmet.original(), snapshot.helmet());
        assertNotSame(chestplate.original(), snapshot.chestplate());
        assertNotSame(leggings.original(), snapshot.leggings());
        assertNotSame(boots.original(), snapshot.boots());
        assertNotSame(offhand.original(), snapshot.offhand());
    }

    @Test void mutatingSnapshotDoesNotMutateTargetItem() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        CloneChain storage = cloneChain(12);
        CloneChain armor = cloneChain(1);
        CloneChain offhand = cloneChain(1);
        ItemStack[] storageContents = new ItemStack[36];
        Arrays.fill(storageContents, storage.original());

        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Target");
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getStorageContents()).thenReturn(storageContents);
        when(inventory.getHelmet()).thenReturn(armor.original());
        when(inventory.getChestplate()).thenReturn(armor.original());
        when(inventory.getLeggings()).thenReturn(armor.original());
        when(inventory.getBoots()).thenReturn(armor.original());
        when(inventory.getItemInOffHand()).thenReturn(offhand.original());

        InventoryInspectionService.Snapshot snapshot = service().snapshot(player);
        snapshot.storage().getFirst().setAmount(1);

        assertEquals(12, storage.original().getAmount());
        verify(storage.original(), never()).setAmount(1);
    }

    @Test void leftEditSwapsLiveSlotAndCursorUsingClones() {
        StackClone current = stack(Material.DIAMOND, 5);
        StackClone cursor = stack(Material.EMERALD, 2);

        InventoryInspectionService.EditResult result = InventoryInspectionService.leftEdit(current.original(), cursor.original());

        assertTrue(result.changed());
        assertSame(cursor.copy(), result.slot());
        assertSame(current.copy(), result.cursor());
    }

    @Test void leftEditIsNoOpForEquivalentStacks() {
        ItemStack first = mock(ItemStack.class);
        ItemStack second = mock(ItemStack.class);
        when(first.getType()).thenReturn(Material.DIAMOND);
        when(second.getType()).thenReturn(Material.DIAMOND);
        when(first.getAmount()).thenReturn(3);
        when(second.getAmount()).thenReturn(3);
        when(first.isSimilar(second)).thenReturn(true);
        when(first.clone()).thenReturn(mock(ItemStack.class));
        when(second.clone()).thenReturn(mock(ItemStack.class));

        InventoryInspectionService.EditResult result = InventoryInspectionService.leftEdit(first, second);

        assertFalse(result.changed());
    }

    @Test void rightEditSplitsTargetStackOntoEmptyCursor() {
        ItemStack current = mock(ItemStack.class);
        ItemStack slotCopy = mock(ItemStack.class);
        ItemStack cursorCopy = mock(ItemStack.class);
        when(current.getType()).thenReturn(Material.DIAMOND);
        when(current.getAmount()).thenReturn(5);
        when(current.clone()).thenReturn(slotCopy, cursorCopy);

        InventoryInspectionService.EditResult result = InventoryInspectionService.rightEdit(current, null);

        assertTrue(result.changed());
        assertSame(slotCopy, result.slot());
        assertSame(cursorCopy, result.cursor());
        verify(slotCopy).setAmount(2);
        verify(cursorCopy).setAmount(3);
    }

    @Test void rightEditPlacesOneItemIntoEmptyTargetSlot() {
        ItemStack cursor = mock(ItemStack.class);
        ItemStack slotCopy = mock(ItemStack.class);
        ItemStack cursorCopy = mock(ItemStack.class);
        when(cursor.getType()).thenReturn(Material.EMERALD);
        when(cursor.getAmount()).thenReturn(4);
        when(cursor.clone()).thenReturn(slotCopy, cursorCopy);

        InventoryInspectionService.EditResult result = InventoryInspectionService.rightEdit(null, cursor);

        assertTrue(result.changed());
        verify(slotCopy).setAmount(1);
        verify(cursorCopy).setAmount(3);
    }

    private static InventoryInspectionService service() {
        return new InventoryInspectionService(
                mock(GuiService.class), mock(UtilityMenuItemFactory.class), mock(MessageService.class));
    }

    private static CloneChain cloneChain(int amount) {
        ItemStack original = mock(ItemStack.class);
        ItemStack firstClone = mock(ItemStack.class);
        ItemStack snapshotClone = mock(ItemStack.class);
        when(original.clone()).thenReturn(firstClone);
        when(firstClone.clone()).thenReturn(snapshotClone);
        when(original.getAmount()).thenReturn(amount);
        when(firstClone.getAmount()).thenReturn(amount);
        when(snapshotClone.getAmount()).thenReturn(amount);
        return new CloneChain(original, firstClone, snapshotClone);
    }

    private static StackClone stack(Material material, int amount) {
        ItemStack original = mock(ItemStack.class);
        ItemStack copy = mock(ItemStack.class);
        when(original.getType()).thenReturn(material);
        when(original.getAmount()).thenReturn(amount);
        when(original.clone()).thenReturn(copy);
        return new StackClone(original, copy);
    }

    private record CloneChain(ItemStack original, ItemStack firstClone, ItemStack snapshotClone) { }
    private record StackClone(ItemStack original, ItemStack copy) { }
}
