package com.zpkdxgames.plexonutility.api;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable public snapshot of PlexonUtility AFK state.
 *
 * <p>Offline or otherwise untracked players are represented by
 * {@link #untracked()}: tracked=false, afk=false, reason=NONE and zero idle duration.
 * No monotonic clock implementation detail is exposed through the public API.</p>
 */
public record AfkState(boolean tracked, boolean afk, Reason reason, Duration idleDuration) {
    public AfkState {
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(idleDuration, "idleDuration");
        if (idleDuration.isNegative()) throw new IllegalArgumentException("idleDuration");
        if (!afk && reason != Reason.NONE) throw new IllegalArgumentException("Active state must use reason NONE");
        if (!tracked && (afk || reason != Reason.NONE || !idleDuration.isZero())) {
            throw new IllegalArgumentException("Untracked state must be inactive with zero idle duration");
        }
    }

    public static AfkState untracked() {
        return new AfkState(false, false, Reason.NONE, Duration.ZERO);
    }

    public enum Reason {
        MANUAL,
        TIMEOUT,
        NONE
    }
}
