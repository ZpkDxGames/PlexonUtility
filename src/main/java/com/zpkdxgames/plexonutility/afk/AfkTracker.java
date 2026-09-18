package com.zpkdxgames.plexonutility.afk;

import com.zpkdxgames.plexonutility.api.AfkState;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

/**
 * Thread-safe, in-memory AFK state. Activity updates are allocation-free after first tracking.
 * No persistence or scheduling is owned by this class.
 */
public final class AfkTracker {
    public enum Transition {
        NONE,
        TO_AFK,
        TO_ACTIVE
    }

    private static final class State {
        private final AtomicLong lastActivityNanos;
        private final AtomicBoolean afk = new AtomicBoolean(false);
        private final AtomicReference<AfkState.Reason> reason = new AtomicReference<>(AfkState.Reason.NONE);

        private State(long now) {
            this.lastActivityNanos = new AtomicLong(now);
        }
    }

    private final ConcurrentHashMap<UUID, State> states = new ConcurrentHashMap<>();
    private final LongSupplier clock;

    public AfkTracker() {
        this(System::nanoTime);
    }

    AfkTracker(LongSupplier clock) {
        this.clock = clock;
    }

    public void join(UUID playerId) {
        states.put(playerId, new State(clock.getAsLong()));
    }

    public void ensureTracked(UUID playerId) {
        states.computeIfAbsent(playerId, ignored -> new State(clock.getAsLong()));
    }

    public void quit(UUID playerId) {
        states.remove(playerId);
    }

    public Transition toggle(UUID playerId) {
        State state = state(playerId);
        long now = clock.getAsLong();
        state.lastActivityNanos.lazySet(now);
        while (true) {
            boolean current = state.afk.get();
            boolean next = !current;
            if (state.afk.compareAndSet(current, next)) {
                state.reason.set(next ? AfkState.Reason.MANUAL : AfkState.Reason.NONE);
                return next ? Transition.TO_AFK : Transition.TO_ACTIVE;
            }
        }
    }

    public Transition activity(UUID playerId) {
        State state = state(playerId);
        state.lastActivityNanos.lazySet(clock.getAsLong());
        if (state.afk.compareAndSet(true, false)) {
            state.reason.set(AfkState.Reason.NONE);
            return Transition.TO_ACTIVE;
        }
        return Transition.NONE;
    }

    public Transition evaluateTimeout(UUID playerId, long timeoutNanos, boolean bypass) {
        if (bypass || timeoutNanos <= 0L) return Transition.NONE;
        State state = state(playerId);
        if (state.afk.get()) return Transition.NONE;
        long elapsed = clock.getAsLong() - state.lastActivityNanos.get();
        if (elapsed < timeoutNanos) return Transition.NONE;
        if (state.afk.compareAndSet(false, true)) {
            state.reason.set(AfkState.Reason.TIMEOUT);
            return Transition.TO_AFK;
        }
        return Transition.NONE;
    }

    public boolean isAfk(UUID playerId) {
        State state = states.get(playerId);
        return state != null && state.afk.get();
    }

    public boolean isTracked(UUID playerId) {
        return states.containsKey(playerId);
    }

    public AfkState snapshot(UUID playerId) {
        State state = states.get(playerId);
        if (state == null) return AfkState.untracked();
        long elapsed = Math.max(0L, clock.getAsLong() - state.lastActivityNanos.get());
        boolean afk = state.afk.get();
        return new AfkState(
                true,
                afk,
                afk ? state.reason.get() : AfkState.Reason.NONE,
                Duration.ofNanos(elapsed));
    }

    public int trackedPlayers() {
        return states.size();
    }

    public int afkPlayers() {
        int count = 0;
        for (State state : states.values()) {
            if (state.afk.get()) count++;
        }
        return count;
    }

    public void clear() {
        states.clear();
    }

    long lastActivityNanos(UUID playerId) {
        State state = states.get(playerId);
        return state == null ? Long.MIN_VALUE : state.lastActivityNanos.get();
    }

    private State state(UUID playerId) {
        return states.computeIfAbsent(playerId, ignored -> new State(clock.getAsLong()));
    }
}
