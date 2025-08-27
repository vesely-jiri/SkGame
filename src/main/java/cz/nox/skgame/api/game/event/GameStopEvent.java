package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.GameMode;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GameStopEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    public GameMode getGameMode() {
        return gameMode;
    }
    public Session getSession() {
        return session;
    }

    private GameMode gameMode;
    private Session session;

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
}
