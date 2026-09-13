package com.zpkdxgames.plexonutility.admin.player;

import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerManagementServiceTest {
    @Test void godModeCancelsOrdinaryDamageWithoutPolling() {
        Player player = player(GameMode.SURVIVAL);
        PlayerManagementService service = service();
        service.setGodMode(player, player, true);
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.FALL);

        service.onDamage(event);

        verify(event).setCancelled(true);
        assertTrue(service.isGodMode(player.getUniqueId()));
    }

    @Test void godModeDoesNotInterceptVoidDamage() {
        Player player = player(GameMode.SURVIVAL);
        PlayerManagementService service = service();
        service.setGodMode(player, player, true);
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.VOID);

        service.onDamage(event);

        verify(event, never()).setCancelled(true);
    }

    @Test void survivalFlightIsOwnedAndRestoredByUtility() {
        Player player = player(GameMode.SURVIVAL);
        when(player.isFlying()).thenReturn(true);
        PlayerManagementService service = service();

        assertTrue(service.setFlight(player, player, true));
        assertTrue(service.isFlightManaged(player.getUniqueId()));
        verify(player).setAllowFlight(true);

        assertFalse(service.setFlight(player, player, false));
        assertFalse(service.isFlightManaged(player.getUniqueId()));
        verify(player).setFlying(false);
        verify(player).setAllowFlight(false);
        verify(player).setFallDistance(0.0F);
    }

    @Test void creativeFlightIsNeverDisabledByUtility() {
        Player player = player(GameMode.CREATIVE);
        when(player.getAllowFlight()).thenReturn(true);
        PlayerManagementService service = service();

        assertTrue(service.setFlight(player, player, false));
        verify(player, never()).setAllowFlight(false);
        verify(player, never()).setFlying(false);
    }

    @Test void speedMappingAlwaysStaysInsideBukkitRange() {
        Player player = player(GameMode.SURVIVAL);
        PlayerManagementService service = service();
        service.setSpeed(player, player, PlayerManagementService.SpeedMode.WALK, 10);
        service.setSpeed(player, player, PlayerManagementService.SpeedMode.FLY, 1);
        verify(player).setWalkSpeed(1.0F);
        verify(player).setFlySpeed(0.1F);
    }

    @Test void fullInventoryClearIncludesStorageArmorAndOffhand() {
        Player player = player(GameMode.SURVIVAL);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        when(inventory.getStorageContents()).thenReturn(new ItemStack[] {new ItemStack(Material.STONE), null});
        when(inventory.getArmorContents()).thenReturn(new ItemStack[] {null, new ItemStack(Material.IRON_BOOTS), null, null});
        when(inventory.getItemInOffHand()).thenReturn(new ItemStack(Material.SHIELD));
        PlayerManagementService service = service();

        int cleared = service.clearInventory(player, player);

        org.junit.jupiter.api.Assertions.assertEquals(3, cleared);
        verify(inventory).clear();
        verify(inventory).setArmorContents(org.mockito.ArgumentMatchers.any(ItemStack[].class));
        verify(inventory).setItemInOffHand(org.mockito.ArgumentMatchers.any(ItemStack.class));
    }

    private static PlayerManagementService service() {
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());
        return new PlayerManagementService(() -> config, mock(AdminAuditService.class));
    }

    private static Player player(GameMode mode) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn("Staff");
        when(player.getGameMode()).thenReturn(mode);
        return player;
    }
}
