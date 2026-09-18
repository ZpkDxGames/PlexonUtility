package com.zpkdxgames.plexonutility.admin;

import com.zpkdxgames.plexoncore.scheduler.CoreScheduler;
import com.zpkdxgames.plexonutility.admin.prison.PrisonLocation;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDataStoreTest {
    @TempDir Path tempDir;

    @Test void stateMutationsExposeDurableRevisionAndScheduleOnlyOnChange() {
        Fixture fixture = fixture();
        AdminDataStore store = fixture.store();
        PrisonLocation prison = new PrisonLocation("Survival_World", 1.5, 64.0, -2.5, 90.0F, 10.0F);
        UUID vanished = UUID.randomUUID();

        store.clearPrison();
        store.setVanished(vanished, false);
        verify(fixture.scheduler(), never()).runIo(eq(fixture.plugin()), any(Runnable.class));

        assertTrue(store.setPrison(prison).join().durable());
        assertEquals(prison, store.snapshot().prison());
        verify(fixture.scheduler(), times(1)).runIo(eq(fixture.plugin()), any(Runnable.class));

        store.setPrison(prison);
        verify(fixture.scheduler(), times(1)).runIo(eq(fixture.plugin()), any(Runnable.class));

        assertTrue(store.setVanished(vanished, true).join().durable());
        assertTrue(store.snapshot().vanished().contains(vanished));
        verify(fixture.scheduler(), times(2)).runIo(eq(fixture.plugin()), any(Runnable.class));

        store.setVanished(vanished, true);
        verify(fixture.scheduler(), times(2)).runIo(eq(fixture.plugin()), any(Runnable.class));

        store.setVanished(vanished, false).join();
        store.clearPrison().join();
        assertNull(store.snapshot().prison());
        verify(fixture.scheduler(), times(4)).runIo(eq(fixture.plugin()), any(Runnable.class));

        AdminDataStore.PersistenceStatus status = store.status();
        assertEquals(status.currentRevision(), status.persistedRevision());
        assertFalse(status.dirty());
        assertEquals(AdminDataStore.HealthState.READY, status.health());
    }

    @Test void schemaOneIsBackedUpAndMigratedToSchemaTwo() throws Exception {
        Fixture fixture = fixture();
        Path file = tempDir.resolve("admin-data.yml");
        Files.writeString(file, """
                schema-version: 1
                prison:
                  configured: false
                vanished: []
                """);

        fixture.store().load();

        assertTrue(Files.exists(tempDir.resolve("admin-data.yml.schema1.bak")));
        assertEquals(AdminDataStore.SCHEMA_VERSION,
                org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file.toFile()).getInt("schema-version"));
        assertFalse(fixture.store().status().dirty());
    }

    private Fixture fixture() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        CoreScheduler scheduler = mock(CoreScheduler.class);
        Server server = mock(Server.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getServer()).thenReturn(server);
        when(scheduler.runIo(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return CompletableFuture.completedFuture(null);
        });
        return new Fixture(plugin, scheduler, new AdminDataStore(plugin, scheduler));
    }

    private record Fixture(JavaPlugin plugin, CoreScheduler scheduler, AdminDataStore store) { }
}
