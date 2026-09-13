package com.zpkdxgames.plexonutility.admin.entity;

import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Wither;
import org.bukkit.entity.Wolf;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EntityCleanupServiceTest {
    @Test void defaultAllSweepProtectsSensitiveEntitiesAndBosses() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("Survival_World");
        Monster zombie = mock(Monster.class);
        entity(zombie, EntityType.ZOMBIE);
        Monster named = mock(Monster.class);
        entity(named, EntityType.ZOMBIE);
        when(named.getCustomName()).thenReturn("Keeper");
        Wolf tamed = mock(Wolf.class);
        entity(tamed, EntityType.WOLF);
        when(tamed.isTamed()).thenReturn(true);
        Villager villager = mock(Villager.class);
        entity(villager, EntityType.VILLAGER);
        ArmorStand armorStand = mock(ArmorStand.class);
        entity(armorStand, EntityType.ARMOR_STAND);
        Display display = mock(Display.class);
        entity(display, EntityType.TEXT_DISPLAY);
        Wither wither = mock(Wither.class);
        entity(wither, EntityType.WITHER);
        Player player = mock(Player.class);
        entity(player, EntityType.PLAYER);
        when(world.getEntities()).thenReturn(List.of(zombie, named, tamed, villager, armorStand, display, wither, player));

        EntityCleanupService service = service();
        EntityCleanupService.Query query = new EntityCleanupService.Query(EntitySelector.parse("all"), world, null, null);
        EntityCleanupService.Preview preview = service.preview(query);
        EntityCleanupService.Result result = service.execute(mock(Player.class), query);

        assertEquals(7, preview.matched());
        assertEquals(6, preview.protectedCount());
        assertEquals(1, preview.removable());
        assertEquals(1, result.removed());
        verify(zombie).remove();
        verify(named, never()).remove();
        verify(tamed, never()).remove();
        verify(villager, never()).remove();
        verify(armorStand, never()).remove();
        verify(display, never()).remove();
        verify(wither, never()).remove();
        verify(player, never()).remove();
    }

    @Test void explicitBossCategoryCanRemoveBoss() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("Survival_World");
        Wither wither = mock(Wither.class);
        entity(wither, EntityType.WITHER);
        when(world.getEntities()).thenReturn(List.of(wither));
        EntityCleanupService service = service();
        EntityCleanupService.Query query = new EntityCleanupService.Query(EntitySelector.parse("bosses"), world, null, null);

        EntityCleanupService.Result result = service.execute(mock(Player.class), query);

        assertEquals(1, result.removed());
        verify(wither).remove();
    }

    @Test void radiusUsesSphericalFilteringInsideConfiguredWorld() {
        World world = mock(World.class);
        when(world.getName()).thenReturn("Survival_World");
        Monster inside = mock(Monster.class);
        entity(inside, EntityType.ZOMBIE);
        when(inside.getLocation()).thenReturn(new Location(world, 3, 64, 4));
        Monster outside = mock(Monster.class);
        entity(outside, EntityType.ZOMBIE);
        when(outside.getLocation()).thenReturn(new Location(world, 9, 64, 9));
        Location center = new Location(world, 0, 64, 0);
        when(world.getNearbyEntities(center, 8.0, 8.0, 8.0)).thenReturn(List.of(inside, outside));
        EntityCleanupService service = service();
        EntityCleanupService.Query query = new EntityCleanupService.Query(EntitySelector.parse("zombie"), world, center, 8.0);

        EntityCleanupService.Result result = service.execute(mock(Player.class), query);

        assertEquals(1, result.removed());
        verify(inside).remove();
        verify(outside, never()).remove();
    }

    private static EntityCleanupService service() {
        UtilityConfig config = UtilityConfig.from(new YamlConfiguration());
        return new EntityCleanupService(() -> config, mock(AdminAuditService.class));
    }

    private static void entity(Entity entity, EntityType type) {
        when(entity.getType()).thenReturn(type);
        when(entity.getScoreboardTags()).thenReturn(Set.of());
    }
}
