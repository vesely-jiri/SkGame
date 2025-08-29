package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class GameStartEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private MiniGame miniGame;
    private Session session;

    public GameStartEvent(Session session, MiniGame miniGame) {
        this.session = session;
        this.miniGame = miniGame;
    }

    public Session getSession() {
        return this.session;
    }

    public MiniGame getMiniGame() {
        return this.miniGame;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
