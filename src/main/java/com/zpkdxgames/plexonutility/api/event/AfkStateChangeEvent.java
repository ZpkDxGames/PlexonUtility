package com.zpkdxgames.plexonutility.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired on the primary thread whenever PlexonUtility changes a player's AFK state.
 * This is informational and intentionally non-cancellable so other PlexonFamily modules can react
 * without polling PlexonUtility or duplicating AFK detection.
 */
public final class AfkStateChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final boolean afk;
    private final Reason reason;

    public AfkStateChangeEvent(Player player, boolean afk, Reason reason) {
        this.player = player;
        this.afk = afk;
        this.reason = reason;
    }

    public Player player() {
        return player;
    }

    public boolean afk() {
        return afk;
    }

    public Reason reason() {
        return reason;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }

    public enum Reason {
        MANUAL,
        TIMEOUT,
        ACTIVITY
    }
}
