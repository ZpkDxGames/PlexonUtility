package com.zpkdxgames.plexonutility.afk;

/** Owns at most one repeating task for the complete AFK subsystem. */
public final class SharedScheduler implements AutoCloseable {
    @FunctionalInterface
    public interface TaskHandle {
        void cancel();
    }

    @FunctionalInterface
    public interface Factory {
        TaskHandle schedule(long periodTicks, Runnable task);
    }

    private final Factory factory;
    private TaskHandle task;

    public SharedScheduler(Factory factory) {
        this.factory = factory;
    }

    public synchronized void restart(long periodTicks, Runnable runnable) {
        if (periodTicks <= 0L) throw new IllegalArgumentException("periodTicks must be positive");
        stop();
        task = factory.schedule(periodTicks, runnable);
    }

    public synchronized void stop() {
        if (task == null) return;
        task.cancel();
        task = null;
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
