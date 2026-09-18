package com.zpkdxgames.plexonutility.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Synchronous presentation request emitted after a committed vanish transition.
 *
 * <p>This is not a Bukkit join/quit lifecycle event. A presentation plugin such as PlexonChats may
 * render the synthetic presence message and mark this request handled. If nobody handles it,
 * PlexonUtility falls back to its configured Core TextService presentation.</p>
 */
public final class SyntheticPresencePresentationEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Transition transition;
    private final AudienceContext audienceContext;
    private boolean handled;

    public SyntheticPresencePresentationEvent(Player player, Transition transition,
                                              AudienceContext audienceContext) {
        super(false);
        this.player = Objects.requireNonNull(player, "player");
        this.transition = Objects.requireNonNull(transition, "transition");
        this.audienceContext = Objects.requireNonNull(audienceContext, "audienceContext");
    }

    public Player player() { return player; }
    public Transition transition() { return transition; }
    public AudienceContext audienceContext() { return audienceContext; }
    public boolean handled() { return handled; }
    public void setHandled(boolean handled) { this.handled = handled; }

    @Override
    public @NotNull HandlerList getHandlers() { return HANDLERS; }

    public static @NotNull HandlerList getHandlerList() { return HANDLERS; }

    public enum Transition {
        SYNTHETIC_JOIN,
        SYNTHETIC_QUIT
    }

    public enum AudienceContext {
        ORDINARY_PLAYERS,
        ALL_EXCEPT_SELF
    }
}
