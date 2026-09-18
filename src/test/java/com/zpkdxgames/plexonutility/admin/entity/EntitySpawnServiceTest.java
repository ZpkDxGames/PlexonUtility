package com.zpkdxgames.plexonutility.admin.entity;

import com.zpkdxgames.plexoncore.scheduler.CoreScheduler;
import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.config.UtilityConfig;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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

    @Test void productionBatchSpawnsAtMostTwentyBeforeSchedulingContinuation() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("admin.entity-management.spawnmob.safe-location-search", false);
        UtilityConfig config = UtilityConfig.from(yaml);

        JavaPlugin plugin = mock(JavaPlugin.class);
        when(plugin.isEnabled()).thenReturn(true);
        CoreScheduler scheduler = mock(CoreScheduler.class);
        AtomicReference<Runnable> continuation = new AtomicReference<>();
        when(scheduler.schedulePrimaryObserved(eq(plugin), any(Duration.class), any(Runnable.class)))
                .thenAnswer(invocation -> {
                    continuation.set(invocation.getArgument(2));
                    return new CoreScheduler.ObservedTaskHandle(
                            new CoreScheduler.TaskHandle(() -> { }, () -> false),
                            new CompletableFuture<>());
                });

        Player player = mock(Player.class);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getTargetBlockExact(32)).thenReturn(null);
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(world.isChunkLoaded(anyInt(), anyInt())).thenReturn(true);
        when(world.spawnEntity(any(Location.class), eq(EntityType.ZOMBIE)))
                .thenReturn(mock(org.bukkit.entity.Entity.class));

        EntitySpawnService service = new EntitySpawnService(
                plugin, scheduler, () -> config, mock(AdminAuditService.class));

        CompletableFuture<EntitySpawnService.SpawnResult> result =
                service.spawnBatched(player, player, EntityType.ZOMBIE, 25);

        verify(world, times(EntitySpawnService.MAX_PER_TICK))
                .spawnEntity(any(Location.class), eq(EntityType.ZOMBIE));
        assertFalse(result.isDone());
        assertTrue(continuation.get() != null);

        continuation.get().run();

        assertTrue(result.isDone());
        assertEquals(25, result.join().spawned());
        verify(world, times(25)).spawnEntity(any(Location.class), eq(EntityType.ZOMBIE));
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
