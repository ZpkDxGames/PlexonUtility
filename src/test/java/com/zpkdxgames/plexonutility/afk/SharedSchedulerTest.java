package com.zpkdxgames.plexonutility.afk;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharedSchedulerTest {
    @Test
    void restartNeverLeavesMoreThanOneRepeatingTask() {
        AtomicInteger scheduled = new AtomicInteger();
        AtomicInteger cancelled = new AtomicInteger();
        SharedScheduler scheduler = new SharedScheduler((period, task) -> {
            scheduled.incrementAndGet();
            return cancelled::incrementAndGet;
        });

        scheduler.restart(200L, () -> { });
        assertEquals(1, scheduler.activeTaskCount());
        scheduler.restart(100L, () -> { });

        assertEquals(2, scheduled.get());
        assertEquals(1, cancelled.get());
        assertEquals(1, scheduler.activeTaskCount());
        assertTrue(scheduler.running());
    }

    @Test
    void stopAndCloseCancelTheSingleTaskCleanly() {
        AtomicInteger cancelled = new AtomicInteger();
        SharedScheduler scheduler = new SharedScheduler((period, task) -> cancelled::incrementAndGet);

        scheduler.restart(200L, () -> { });
        scheduler.stop();
        scheduler.stop();

        assertEquals(1, cancelled.get());
        assertEquals(0, scheduler.activeTaskCount());
        assertFalse(scheduler.running());

        scheduler.restart(200L, () -> { });
        scheduler.close();
        assertEquals(2, cancelled.get());
        assertEquals(0, scheduler.activeTaskCount());
    }

    @Test
    void schedulerIsGlobalAndHasNoPlayerIdentitySurface() {
        SharedScheduler scheduler = new SharedScheduler((period, task) -> () -> { });
        scheduler.restart(20L, () -> { });
        assertEquals(1, scheduler.activeTaskCount());
    }
}
