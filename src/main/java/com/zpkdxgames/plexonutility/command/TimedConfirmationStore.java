package com.zpkdxgames.plexonutility.command;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Small one-shot actor-scoped confirmation store with monotonic expiry. */
final class TimedConfirmationStore<T> {
    private final long ttlNanos;
    private final LongSupplier clock;
    private final Map<String, Entry<T>> pending = new ConcurrentHashMap<>();

    TimedConfirmationStore(long ttlNanos, LongSupplier clock) {
        if (ttlNanos <= 0L) throw new IllegalArgumentException("ttlNanos");
        this.ttlNanos = ttlNanos;
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    void put(String actorKey, T value) {
        Objects.requireNonNull(actorKey, "actorKey");
        Objects.requireNonNull(value, "value");
        pending.put(actorKey, new Entry<>(value, clock.getAsLong() + ttlNanos));
    }

    Optional<T> consume(String actorKey) {
        Entry<T> entry = pending.remove(actorKey);
        if (entry == null || clock.getAsLong() > entry.expiresAtNanos()) return Optional.empty();
        return Optional.of(entry.value());
    }

    boolean hasValid(String actorKey) {
        Entry<T> entry = pending.get(actorKey);
        if (entry == null) return false;
        if (clock.getAsLong() <= entry.expiresAtNanos()) return true;
        pending.remove(actorKey, entry);
        return false;
    }

    void clear(String actorKey) {
        pending.remove(actorKey);
    }

    private record Entry<T>(T value, long expiresAtNanos) { }
}
