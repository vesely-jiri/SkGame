package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class SessionSettingsChangedEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Session session;
    /** One of: "minigame" | "map" | "rounds" | "visibility" | "shuffle" | "allow-spectate" */
    private final String key;

    public SessionSettingsChangedEvent(Session session, String key) {
        this.session = session;
        this.key = key;
    }

    public Session getSession() { return session; }
    public String getKey() { return key; }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
