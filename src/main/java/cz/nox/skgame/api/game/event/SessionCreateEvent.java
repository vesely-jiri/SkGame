package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class SessionCreateEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Session session;

    public SessionCreateEvent(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return this.session;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
