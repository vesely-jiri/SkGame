package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlayerScoreChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Session session;
    private final Player player;
    private final int oldScore;
    private final int newScore;
    private final int delta;

    public PlayerScoreChangeEvent(Session session, Player player, int oldScore, int newScore) {
        this.session = session;
        this.player = player;
        this.oldScore = oldScore;
        this.newScore = newScore;
        this.delta = newScore - oldScore;
    }

    public Session getSession() { return session; }
    public Player getPlayer() { return player; }
    public MiniGame getMiniGame() { return session.getMiniGame(); }
    public int getOldScore() { return oldScore; }
    public int getNewScore() { return newScore; }
    public int getDelta() { return delta; }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
