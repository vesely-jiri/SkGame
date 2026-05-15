package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.DisbandReason;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class SessionDisbandEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Session session;
    private final DisbandReason reason;

    /** Legacy compat — callers that don't know the reason get EXPLICIT_DISBAND. M3 updates these. */
    public SessionDisbandEvent(Session session) {
        this(session, DisbandReason.EXPLICIT_DISBAND);
    }

    public SessionDisbandEvent(Session session, DisbandReason reason) {
        this.session = session;
        this.reason = reason;
    }

    public Session getSession() { return session; }
    public DisbandReason getReason() { return reason; }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
