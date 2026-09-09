package com.zpkdxgames.plexonutility.cooldown;

import com.zpkdxgames.plexonutility.feature.Feature;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CooldownServiceTest {
    @Test
    void tracksRemainingTimeWithMonotonicClock() {
        AtomicLong now = new AtomicLong(1_000L);
        CooldownService service = new CooldownService(now::get);
        UUID player = UUID.randomUUID();

        service.start(player, Feature.FEED, 500L);
        assertEquals(500L, service.remainingNanos(player, Feature.FEED));

        now.addAndGet(200L);
        assertEquals(300L, service.remainingNanos(player, Feature.FEED));

        now.addAndGet(400L);
        assertEquals(0L, service.remainingNanos(player, Feature.FEED));
    }

    @Test
    void quitCleanupRemovesPlayerState() {
        AtomicLong now = new AtomicLong(10L);
        CooldownService service = new CooldownService(now::get);
        UUID player = UUID.randomUUID();

        service.start(player, Feature.HEAL, 100L);
        assertEquals(1, service.trackedPlayers());
        service.clear(player);
        assertEquals(0, service.trackedPlayers());
        assertEquals(0L, service.remainingNanos(player, Feature.HEAL));
    }

    @Test
    void disabledCooldownCreatesNoState() {
        CooldownService service = new CooldownService(() -> 1L);
        service.start(UUID.randomUUID(), Feature.FEED, 0L);
        assertEquals(0, service.trackedPlayers());
    }
}
