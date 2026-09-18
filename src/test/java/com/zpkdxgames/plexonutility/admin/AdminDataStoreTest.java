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
import java.time.Duration;
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

    @Test void failedWriteRetriesBecomesDegradedAndLaterFullSnapshotRecovers() throws Exception {
        Path blocked = tempDir.resolve("blocked-data-folder");
        Files.writeString(blocked, "not-a-directory");

        JavaPlugin plugin = mock(JavaPlugin.class);
        CoreScheduler scheduler = mock(CoreScheduler.class);
        Server server = mock(Server.class);
        when(plugin.getDataFolder()).thenReturn(blocked.toFile());
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("AdminDataStoreFailureTest"));
        when(scheduler.runIo(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
            try {
                ((Runnable) invocation.getArgument(1)).run();
                return CompletableFuture.completedFuture(null);
            } catch (Throwable failure) {
                return CompletableFuture.failedFuture(failure);
            }
        });

        AdminDataStore store = new AdminDataStore(plugin, scheduler);
        PrisonLocation prison = new PrisonLocation("Survival_World", 1, 64, 1, 0, 0);

        AdminDataStore.PersistenceResult failed = store.setPrison(prison).join();

        assertFalse(failed.durable());
        assertEquals(AdminDataStore.HealthState.DEGRADED, store.status().health());
        assertTrue(store.status().dirty());
        assertEquals(2, store.status().retryCount());
        assertTrue(store.status().lastFailure() != null && !store.status().lastFailure().isBlank());

        Files.delete(blocked);
        Files.createDirectories(blocked);
        AdminDataStore.PersistenceResult recovered = store.setVanished(UUID.randomUUID(), true).join();

        assertTrue(recovered.durable());
        assertEquals(AdminDataStore.HealthState.READY, store.status().health());
        assertFalse(store.status().dirty());
        assertEquals(store.status().currentRevision(), store.status().persistedRevision());
    }

    @Test void boundedCloseReturnsWithDirtyStateWhenIoNeverCompletes() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        CoreScheduler scheduler = mock(CoreScheduler.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("AdminDataStoreCloseTest"));

        CompletableFuture<Void> stalled = new CompletableFuture<>();
        when(scheduler.runIo(eq(plugin), any(Runnable.class))).thenReturn(stalled);
        AdminDataStore store = new AdminDataStore(plugin, scheduler);
        store.setPrison(new PrisonLocation("Survival_World", 1, 64, 1, 0, 0));

        long started = System.nanoTime();
        store.close(Duration.ofMillis(20));
        long elapsedMillis = java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertTrue(elapsedMillis < 1_000L);
        assertTrue(store.status().dirty());
        assertEquals(AdminDataStore.HealthState.CLOSED, store.status().health());
        assertEquals(1, store.status().pendingWrites());
    }

    private Fixture fixture() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        CoreScheduler scheduler = mock(CoreScheduler.class);
        Server server = mock(Server.class);
        when(plugin.getDataFolder()).thenReturn(tempDir.toFile());
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(java.util.logging.Logger.getLogger("AdminDataStoreTest"));
        when(scheduler.runIo(eq(plugin), any(Runnable.class))).thenAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return CompletableFuture.completedFuture(null);
        });
        return new Fixture(plugin, scheduler, new AdminDataStore(plugin, scheduler));
    }

    private record Fixture(JavaPlugin plugin, CoreScheduler scheduler, AdminDataStore store) { }
}
