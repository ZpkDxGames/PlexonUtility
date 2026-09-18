package com.zpkdxgames.plexonutility.admin.prison;

import com.zpkdxgames.plexonutility.admin.AdminAuditService;
import com.zpkdxgames.plexonutility.admin.AdminDataStore;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrisonServiceTest {
    @Test void setPreservesWorldIdentityCoordinatesRotationAndWaitsForDurability() {
        Fixture fixture = fixture();
        Player actor = mock(Player.class);
        World world = mock(World.class);
        UUID worldId = UUID.randomUUID();
        when(world.getUID()).thenReturn(worldId);
        when(world.getName()).thenReturn("Survival_World");
        when(actor.getLocation()).thenReturn(new Location(world, 1.5, 64.25, -8.75, 91.0F, -12.5F));
        when(fixture.data.setPrison(any(PrisonLocation.class))).thenReturn(
                CompletableFuture.completedFuture(new AdminDataStore.PersistenceResult(1L, true, "persisted")));

        PrisonLocation result = fixture.service.set(actor).join();

        assertEquals(worldId, result.worldId());
        assertEquals("Survival_World", result.worldName());
        assertEquals(1.5, result.x());
        verify(fixture.data).setPrison(result);
        verify(fixture.audit).log("PRISON_SET", actor,
                "world=Survival_World coordinates=2, 64, -9 revision=1");
    }

    @Test void gotoDistinguishesNotConfiguredAndMissingWorld() {
        Fixture fixture = fixture();
        Player actor = onlinePlayer();
        when(fixture.data.snapshot()).thenReturn(AdminDataStore.Snapshot.empty());
        assertEquals(PrisonService.Result.NOT_CONFIGURED, fixture.service.gotoPrison(actor).join());

        PrisonLocation location = location();
        when(fixture.data.snapshot()).thenReturn(new AdminDataStore.Snapshot(location, Set.of()));
        when(fixture.server.getWorld("Survival_World")).thenReturn(null);
        assertEquals(PrisonService.Result.WORLD_MISSING, fixture.service.gotoPrison(actor).join());
    }

    @Test void gotoUsesTeleportAsyncAndCompletesAfterSuccess() {
        Fixture fixture = fixture();
        Player actor = onlinePlayer();
        World world = mock(World.class);
        PrisonLocation location = location();
        when(fixture.data.snapshot()).thenReturn(new AdminDataStore.Snapshot(location, Set.of()));
        when(fixture.server.getWorld("Survival_World")).thenReturn(world);
        when(actor.teleportAsync(any(Location.class))).thenReturn(CompletableFuture.completedFuture(true));

        assertEquals(PrisonService.Result.SUCCESS, fixture.service.gotoPrison(actor).join());

        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(actor).teleportAsync(captor.capture());
        assertEquals(5.5, captor.getValue().getX());
        assertEquals(70.0, captor.getValue().getY());
        assertEquals(-3.5, captor.getValue().getZ());
    }

    @Test void sendRejectsTargetDisconnectRaceBeforeTeleport() {
        Fixture fixture = fixture();
        CommandSender actor = mock(CommandSender.class);
        Player target = mock(Player.class);
        when(target.isOnline()).thenReturn(false);

        assertEquals(PrisonService.Result.TARGET_OFFLINE, fixture.service.send(actor, target).join());
        verify(target, never()).teleportAsync(any(Location.class));
    }

    @Test void sendRevalidatesDisconnectAfterAsyncTeleport() {
        Fixture fixture = fixture();
        CommandSender actor = mock(CommandSender.class);
        Player target = mock(Player.class);
        World world = mock(World.class);
        PrisonLocation location = location();
        when(fixture.data.snapshot()).thenReturn(new AdminDataStore.Snapshot(location, Set.of()));
        when(fixture.server.getWorld("Survival_World")).thenReturn(world);
        when(target.isOnline()).thenReturn(true, false);
        when(target.isConnected()).thenReturn(true);
        when(target.teleportAsync(any(Location.class))).thenReturn(CompletableFuture.completedFuture(true));

        assertEquals(PrisonService.Result.TARGET_OFFLINE, fixture.service.send(actor, target).join());
        verify(fixture.audit, never()).log(
                org.mockito.ArgumentMatchers.eq("PRISON_SEND"),
                any(CommandSender.class), any(Player.class), any(String.class));
    }

    @Test void clearReportsSuccessOnlyAfterDurableWrite() {
        Fixture fixture = fixture();
        CommandSender actor = mock(CommandSender.class);
        PrisonLocation location = location();
        when(fixture.data.snapshot()).thenReturn(new AdminDataStore.Snapshot(location, Set.of()));
        when(fixture.data.clearPrison()).thenReturn(
                CompletableFuture.completedFuture(new AdminDataStore.PersistenceResult(2L, true, "persisted")));

        assertTrue(fixture.service.clear(actor).join());
        verify(fixture.audit).log("PRISON_CLEAR", actor,
                "world=Survival_World coordinates=6, 70, -3 revision=2");

        when(fixture.data.snapshot()).thenReturn(AdminDataStore.Snapshot.empty());
        assertFalse(fixture.service.clear(actor).join());
    }

    private static PrisonLocation location() {
        return new PrisonLocation("Survival_World", 5.5, 70.0, -3.5, 45.0F, 8.0F);
    }

    private static Player onlinePlayer() {
        Player player = mock(Player.class);
        when(player.isOnline()).thenReturn(true);
        when(player.isConnected()).thenReturn(true);
        return player;
    }

    private static Fixture fixture() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        Server server = mock(Server.class);
        AdminDataStore data = mock(AdminDataStore.class);
        AdminAuditService audit = mock(AdminAuditService.class);
        when(plugin.getServer()).thenReturn(server);
        return new Fixture(new PrisonService(plugin, data, audit), data, audit, server);
    }

    private record Fixture(PrisonService service, AdminDataStore data, AdminAuditService audit, Server server) { }
}
