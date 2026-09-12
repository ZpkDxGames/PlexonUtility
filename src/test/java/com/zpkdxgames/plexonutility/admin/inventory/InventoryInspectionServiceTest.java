package com.zpkdxgames.plexonutility.admin.inventory;

import com.zpkdxgames.plexoncore.gui.GuiService;
import com.zpkdxgames.plexonutility.menu.UtilityMenuItemFactory;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
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

    private record CloneChain(ItemStack original, ItemStack firstClone, ItemStack snapshotClone) { }
}
