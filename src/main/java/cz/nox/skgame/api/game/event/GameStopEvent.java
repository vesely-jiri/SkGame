package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GameStopEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private MiniGame miniGame;
    private Session session;
    private String reason;

    public GameStopEvent(MiniGame miniGame, Session session, String reason) {
        this.miniGame = miniGame;
        this.session = session;
        this.reason = reason;
    }

    public MiniGame getMiniGame() {
        return miniGame;
    }
    public Session getSession() {
        return session;
    }
    public String getReason() {
        return reason;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
