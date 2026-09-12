package com.zpkdxgames.plexonutility.admin.inventory;

import com.zpkdxgames.plexoncore.gui.GuiService;
import com.zpkdxgames.plexonutility.menu.UtilityMenuItemFactory;
import com.zpkdxgames.plexonutility.message.MessageService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventoryInspectionServiceTest {
    @Test void snapshotClonesStorageArmorAndOffhand() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        UUID id = UUID.randomUUID();
        ItemStack storage = new ItemStack(Material.DIAMOND, 3);
        ItemStack helmet = new ItemStack(Material.DIAMOND_HELMET);
        ItemStack offhand = new ItemStack(Material.SHIELD);
        when(player.getUniqueId()).thenReturn(id);
        when(player.getName()).thenReturn("Target");
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{storage});
        when(inventory.getHelmet()).thenReturn(helmet);
        when(inventory.getItemInOffHand()).thenReturn(offhand);

        InventoryInspectionService service = new InventoryInspectionService(
                mock(GuiService.class), mock(UtilityMenuItemFactory.class), mock(MessageService.class));
        InventoryInspectionService.Snapshot snapshot = service.snapshot(player);

        assertEquals(id, snapshot.targetId());
        assertEquals(36, snapshot.storage().size());
        assertEquals(3, snapshot.storage().getFirst().getAmount());
        assertNotSame(storage, snapshot.storage().getFirst());
        assertNotSame(helmet, snapshot.helmet());
        assertNotSame(offhand, snapshot.offhand());
    }

    @Test void mutatingSnapshotDoesNotMutateTargetItem() {
        Player player = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        ItemStack original = new ItemStack(Material.STONE, 12);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Target");
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[]{original});
        when(inventory.getItemInOffHand()).thenReturn(new ItemStack(Material.AIR));

        InventoryInspectionService service = new InventoryInspectionService(
                mock(GuiService.class), mock(UtilityMenuItemFactory.class), mock(MessageService.class));
        InventoryInspectionService.Snapshot snapshot = service.snapshot(player);
        snapshot.storage().getFirst().setAmount(1);

        assertEquals(12, original.getAmount());
    }
}
