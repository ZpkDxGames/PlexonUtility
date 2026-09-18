package com.zpkdxgames.plexonutility.afk;

import java.util.Objects;

/**
 * Owns at most one scheduled AFK coordinator. The coordinator is intentionally expressed as
 * owner-scoped one-shot scheduling so PlexonCore can purge/observe every active task.
 */
public final class SharedScheduler implements AutoCloseable {
    @FunctionalInterface
    public interface TaskHandle {
        void cancel();
    }

    @FunctionalInterface
    public interface Factory {
        TaskHandle schedule(long delayTicks, Runnable task);
    }

    private final Factory factory;
    private TaskHandle task;
    private Runnable action;
    private long periodTicks;
    private long generation;

    public SharedScheduler(Factory factory) {
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    public synchronized void restart(long periodTicks, Runnable runnable) {
        if (periodTicks <= 0L) throw new IllegalArgumentException("periodTicks must be positive");
        Objects.requireNonNull(runnable, "runnable");
        stopInternal();
        this.periodTicks = periodTicks;
        this.action = runnable;
        long token = ++generation;
        scheduleNext(token);
    }

    public synchronized void stop() {
        generation++;
        stopInternal();
        action = null;
    }

    private void stopInternal() {
        if (task == null) return;
        task.cancel();
        task = null;
    }

    private synchronized void scheduleNext(long token) {
        if (token != generation || action == null) return;
        task = factory.schedule(periodTicks, () -> runCycle(token));
    }

    private void runCycle(long token) {
        Runnable current;
        synchronized (this) {
            if (token != generation || action == null) return;
            current = action;
        }

        try {
            current.run();
        } finally {
            synchronized (this) {
                if (token != generation || action == null) {
                    task = null;
                    return;
                }
                scheduleNext(token);
            }
        }
    }

    public synchronized boolean running() {
        return task != null;
    }

    public synchronized int activeTaskCount() {
        return task == null ? 0 : 1;
    }

    @Override
    public void close() {
        stop();
    }
}
