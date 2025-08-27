package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.GameMode;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GameStartEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private GameMode gameMode;
    private Session session;

    public GameStartEvent(Session session, GameMode gameMode) {
        this.session = session;
        this.gameMode = gameMode;
    }

    public Session getSession() {
        return this.session;
    }

    public GameMode getGameMode() {
        return this.gameMode;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
