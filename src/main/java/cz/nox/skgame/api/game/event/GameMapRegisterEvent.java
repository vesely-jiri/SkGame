package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.GameMap;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GameMapRegisterEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private GameMap gameMap;

    public GameMapRegisterEvent(GameMap gameMap) {
        this.gameMap = gameMap;
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
