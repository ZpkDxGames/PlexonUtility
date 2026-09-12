package com.zpkdxgames.plexonutility.admin;

import com.zpkdxgames.plexoncore.scheduler.CoreScheduler;
import com.zpkdxgames.plexonutility.admin.prison.PrisonLocation;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminDataStoreTest {
    @Test void stateMutationsScheduleIoOnlyWhenSnapshotActuallyChanges() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        CoreScheduler scheduler = mock(CoreScheduler.class);
        when(plugin.getDataFolder()).thenReturn(new File("target/admin-data-store-test"));
        when(scheduler.runIo(any(Runnable.class))).thenReturn(CompletableFuture.completedFuture(null));
        AdminDataStore store = new AdminDataStore(plugin, scheduler);
        PrisonLocation prison = new PrisonLocation("Survival_World", 1.5, 64.0, -2.5, 90.0F, 10.0F);
        UUID vanished = UUID.randomUUID();

        store.clearPrison();
        store.setVanished(vanished, false);
        verify(scheduler, never()).runIo(any(Runnable.class));

        store.setPrison(prison);
        assertEquals(prison, store.snapshot().prison());
        verify(scheduler, times(1)).runIo(any(Runnable.class));

        store.setPrison(prison);
        verify(scheduler, times(1)).runIo(any(Runnable.class));

        store.setVanished(vanished, true);
        assertTrue(store.snapshot().vanished().contains(vanished));
        verify(scheduler, times(2)).runIo(any(Runnable.class));

        store.setVanished(vanished, true);
        verify(scheduler, times(2)).runIo(any(Runnable.class));

        store.setVanished(vanished, false);
        verify(scheduler, times(3)).runIo(any(Runnable.class));

        store.clearPrison();
        assertNull(store.snapshot().prison());
        verify(scheduler, times(4)).runIo(any(Runnable.class));

        store.clearPrison();
        verify(scheduler, times(4)).runIo(any(Runnable.class));
    }
}
