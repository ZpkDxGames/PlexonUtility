package com.zpkdxgames.plexonutility.cooldown;

import com.zpkdxgames.plexonutility.feature.Feature;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

public final class CooldownService {
    private final Map<UUID, EnumMap<Feature, Long>> availableAt = new HashMap<>();
    private final LongSupplier nanoTime;

    public CooldownService() {
        this(System::nanoTime);
    }

    CooldownService(LongSupplier nanoTime) {
        this.nanoTime = nanoTime;
    }

    public long remainingNanos(UUID playerId, Feature feature) {
        EnumMap<Feature, Long> player = availableAt.get(playerId);
        if (player == null) return 0L;
        long until = player.getOrDefault(feature, 0L);
        return Math.max(0L, until - nanoTime.getAsLong());
    }

    public void start(UUID playerId, Feature feature, long durationNanos) {
        if (durationNanos <= 0L) return;
        long now = nanoTime.getAsLong();
        availableAt.computeIfAbsent(playerId, ignored -> new EnumMap<>(Feature.class))
                .put(feature, Math.addExact(now, durationNanos));
    }

    public void clear(UUID playerId) {
        availableAt.remove(playerId);
    }

    public int trackedPlayers() {
        return availableAt.size();
    }

    public void clearAll() {
        availableAt.clear();
    }
}
