package com.zpkdxgames.plexonutility.admin.entity;

import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntitySpawnServiceTest {
    @Test void validLivingSpawnableTypeParses() {
        assertEquals(EntityType.ZOMBIE, service(new YamlConfiguration()).parseType("zombie"));
    }

    @Test void playerTypeIsAlwaysRejected() {
        assertThrows(IllegalArgumentException.class, () -> service(new YamlConfiguration()).parseType("player"));
    }

    @Test void configCanBlockSpecificSpawnableType() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("admin.entity-management.spawnmob.blocked-entity-types", java.util.List.of("zombie"));
        EntitySpawnService service = service(yaml);
        assertThrows(IllegalArgumentException.class, () -> service.parseType("zombie"));
        assertFalse(service.allowedTypeNames().contains("zombie"));
    }

    @Test void configuredMaximumRejectsLargerBatchBeforeWorldWork() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("admin.entity-management.spawnmob.max-amount", 25);
        EntitySpawnService service = service(yaml);
        Player player = mock(Player.class);
        assertThrows(IllegalArgumentException.class,
                () -> service.spawn(player, player, EntityType.ZOMBIE, 26));
    }

    @Test void noLoadedSafeLocationFailsCleanlyWithoutChunkFanout() {
        EntitySpawnService service = service(new YamlConfiguration());
        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getTargetBlockExact(32)).thenReturn(null);
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(world.isChunkLoaded(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt())).thenReturn(false);

        EntitySpawnService.SpawnResult result = service.spawn(player, player, EntityType.ZOMBIE, 1);

        assertEquals(0, result.spawned());
        assertEquals(1, result.failed());
        assertNull(result.location());
    }

    @Test void completionUsesRegistryAndIncludesCommonSpawnableMob() {
        EntitySpawnService service = service(new YamlConfiguration());
        assertTrue(service.allowedTypeNames().contains("zombie"));
        assertFalse(service.allowedTypeNames().contains("player"));
    }

    private static EntitySpawnService service(YamlConfiguration yaml) {
        UtilityConfig config = UtilityConfig.from(yaml);
        return new EntitySpawnService(() -> config, mock(AdminAuditService.class));
    }
}
