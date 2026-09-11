package com.zpkdxgames.plexonutility.afk;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AfkTrackerTest {
    @Test
    void manualToggleMovesActiveToAfkAndBackToActive() {
        AtomicLong clock = new AtomicLong(1_000L);
        AfkTracker tracker = new AfkTracker(clock::get);
        UUID player = UUID.randomUUID();
        tracker.join(player);

        assertEquals(AfkTracker.Transition.TO_AFK, tracker.toggle(player));
        assertTrue(tracker.isAfk(player));
        assertEquals(AfkTracker.Transition.TO_ACTIVE, tracker.toggle(player));
        assertFalse(tracker.isAfk(player));
    }

    @Test
    void autoTimeoutMarksIdlePlayerAfk() {
        AtomicLong clock = new AtomicLong(10L);
        AfkTracker tracker = new AfkTracker(clock::get);
        UUID player = UUID.randomUUID();
        tracker.join(player);

        clock.set(1_010L);
        assertEquals(AfkTracker.Transition.TO_AFK, tracker.evaluateTimeout(player, 1_000L, false));
        assertTrue(tracker.isAfk(player));
    }

    @Test
    void automaticBypassPreventsTimeout() {
        AtomicLong clock = new AtomicLong(10L);
        AfkTracker tracker = new AfkTracker(clock::get);
        UUID player = UUID.randomUUID();
        tracker.join(player);

        clock.set(10_000L);
        assertEquals(AfkTracker.Transition.NONE, tracker.evaluateTimeout(player, 1_000L, true));
        assertFalse(tracker.isAfk(player));
    }

    @Test
    void meaningfulActivityClearsAfkAndRefreshesTimestamp() {
        AtomicLong clock = new AtomicLong(100L);
        AfkTracker tracker = new AfkTracker(clock::get);
        UUID player = UUID.randomUUID();
        tracker.join(player);
        tracker.toggle(player);

        clock.set(500L);
        assertEquals(AfkTracker.Transition.TO_ACTIVE, tracker.activity(player));
        assertFalse(tracker.isAfk(player));
        assertEquals(500L, tracker.lastActivityNanos(player));
    }

    @Test
    void ordinaryActivityDoesNotCreateTransitionsOrPersistenceWork() {
        AtomicLong clock = new AtomicLong(100L);
        AfkTracker tracker = new AfkTracker(clock::get);
        UUID player = UUID.randomUUID();
        tracker.join(player);

        clock.set(200L);
        assertEquals(AfkTracker.Transition.NONE, tracker.activity(player));
        assertEquals(200L, tracker.lastActivityNanos(player));
        assertEquals(1, tracker.trackedPlayers());
        assertEquals(0, tracker.afkPlayers());
    }

    @Test
    void joinStartsActiveAndQuitRemovesRuntimeState() {
        AfkTracker tracker = new AfkTracker();
        UUID player = UUID.randomUUID();

        tracker.join(player);
        assertTrue(tracker.isTracked(player));
        assertFalse(tracker.isAfk(player));

        tracker.quit(player);
        assertFalse(tracker.isTracked(player));
        assertEquals(0, tracker.trackedPlayers());
    }

    @Test
    void clearResetsAllEphemeralStateForPluginDisable() {
        AfkTracker tracker = new AfkTracker();
        UUID one = UUID.randomUUID();
        UUID two = UUID.randomUUID();
        tracker.join(one);
        tracker.join(two);
        tracker.toggle(one);

        tracker.clear();

        assertEquals(0, tracker.trackedPlayers());
        assertEquals(0, tracker.afkPlayers());
    }
}
