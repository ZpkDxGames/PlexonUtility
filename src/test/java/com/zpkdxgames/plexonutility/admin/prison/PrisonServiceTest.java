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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PrisonServiceTest {
    @Test void setPreservesWorldCoordinatesAndRotationAndAudits() {
        Fixture fixture = fixture();
        Player actor = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("Survival_World");
        when(actor.getLocation()).thenReturn(new Location(world, 1.5, 64.25, -8.75, 91.0F, -12.5F));

        PrisonLocation result = fixture.service.set(actor);

        assertEquals(new PrisonLocation("Survival_World", 1.5, 64.25, -8.75, 91.0F, -12.5F), result);
        verify(fixture.data).setPrison(result);
        verify(fixture.audit).log("PRISON_SET", actor, "world=Survival_World coordinates=2, 64, -9");
    }

    @Test void gotoDistinguishesNotConfiguredAndMissingWorld() {
        Fixture fixture = fixture();
        Player actor = mock(Player.class);
        when(fixture.data.snapshot()).thenReturn(AdminDataStore.Snapshot.empty());
        assertEquals(PrisonService.Result.NOT_CONFIGURED, fixture.service.gotoPrison(actor));

        PrisonLocation location = location();
        when(fixture.data.snapshot()).thenReturn(new AdminDataStore.Snapshot(location, Set.of()));
        when(fixture.server.getWorld("Survival_World")).thenReturn(null);
        assertEquals(PrisonService.Result.WORLD_MISSING, fixture.service.gotoPrison(actor));
    }

    @Test void gotoTeleportsToResolvedLocation() {
        Fixture fixture = fixture();
        Player actor = mock(Player.class);
        World world = mock(World.class);
        PrisonLocation location = location();
        when(fixture.data.snapshot()).thenReturn(new AdminDataStore.Snapshot(location, Set.of()));
        when(fixture.server.getWorld("Survival_World")).thenReturn(world);
        when(actor.teleport(any(Location.class))).thenReturn(true);

        assertEquals(PrisonService.Result.SUCCESS, fixture.service.gotoPrison(actor));

        ArgumentCaptor<Location> captor = ArgumentCaptor.forClass(Location.class);
        verify(actor).teleport(captor.capture());
        assertEquals(5.5, captor.getValue().getX());
        assertEquals(70.0, captor.getValue().getY());
        assertEquals(-3.5, captor.getValue().getZ());
        assertEquals(45.0F, captor.getValue().getYaw());
        assertEquals(8.0F, captor.getValue().getPitch());
    }

    @Test void sendRejectsTargetDisconnectRaceBeforeTeleport() {
        Fixture fixture = fixture();
        CommandSender actor = mock(CommandSender.class);
        Player target = mock(Player.class);
        when(target.isOnline()).thenReturn(false);

        assertEquals(PrisonService.Result.TARGET_OFFLINE, fixture.service.send(actor, target));
        verify(target, never()).teleport(any(Location.class));
    }

    @Test void sendTeleportsAndAuditsSuccessfulAction() {
        Fixture fixture = fixture();
        CommandSender actor = mock(CommandSender.class);
        Player target = mock(Player.class);
        World world = mock(World.class);
        PrisonLocation location = location();
        when(target.isOnline()).thenReturn(true);
        when(fixture.data.snapshot()).thenReturn(new AdminDataStore.Snapshot(location, Set.of()));
        when(fixture.server.getWorld("Survival_World")).thenReturn(world);
        when(target.teleport(any(Location.class))).thenReturn(true);

        assertEquals(PrisonService.Result.SUCCESS, fixture.service.send(actor, target));
        verify(fixture.audit).log("PRISON_SEND", actor, target,
                "world=Survival_World coordinates=6, 70, -3");
    }

    @Test void clearOnlyMutatesWhenConfigured() {
        Fixture fixture = fixture();
        CommandSender actor = mock(CommandSender.class);
        when(fixture.data.snapshot()).thenReturn(AdminDataStore.Snapshot.empty());
        assertFalse(fixture.service.clear(actor));
        verify(fixture.data, never()).clearPrison();

        PrisonLocation location = location();
        when(fixture.data.snapshot()).thenReturn(new AdminDataStore.Snapshot(location, Set.of()));
        assertTrue(fixture.service.clear(actor));
        verify(fixture.data).clearPrison();
        verify(fixture.audit).log("PRISON_CLEAR", actor,
                "world=Survival_World coordinates=6, 70, -3");
    }

    private static PrisonLocation location() {
        return new PrisonLocation("Survival_World", 5.5, 70.0, -3.5, 45.0F, 8.0F);
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
