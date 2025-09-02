package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class GameStartEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private Session session;
    private MiniGame miniGame;
    private GameMap gameMap;

    public GameStartEvent(Session session, MiniGame miniGame, GameMap gameMap) {
        this.session = session;
        this.miniGame = miniGame;
        this.gameMap = gameMap;
    }

    public Session getSession() {
        return this.session;
    }
    public MiniGame getMiniGame() {
        return this.miniGame;
    }
    public GameMap getGameMap() {
        return this.gameMap;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
