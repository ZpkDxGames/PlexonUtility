package com.zpkdxgames.plexonutility.api.event;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** Fired only when PlexonUtility actually changes a player's vanish state. */
public final class VanishStateChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final boolean vanished;
    private final CommandSender actor;
    private final boolean syntheticPresenceRequested;

    public VanishStateChangeEvent(Player player, boolean vanished, CommandSender actor,
                                  boolean syntheticPresenceRequested) {
        this.player = Objects.requireNonNull(player, "player");
        this.vanished = vanished;
        this.actor = actor;
        this.syntheticPresenceRequested = syntheticPresenceRequested;
    }

    public Player player() { return player; }
    public boolean vanished() { return vanished; }
    public CommandSender actor() { return actor; }
    public boolean syntheticPresenceRequested() { return syntheticPresenceRequested; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }
}
