package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RoundEndEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Session session;
    private final int round;
    private final int totalRounds;
    private final boolean hasNextRound;
    private final String reason;

    public RoundEndEvent(Session session, int round, int totalRounds, boolean hasNextRound, String reason) {
        this.session = session;
        this.round = round;
        this.totalRounds = totalRounds;
        this.hasNextRound = hasNextRound;
        this.reason = reason;
    }

    public Session getSession() { return session; }
    public MiniGame getMiniGame() { return session.getMiniGame(); }
    public int getRound() { return round; }
    public int getTotalRounds() { return totalRounds; }
    public boolean hasNextRound() { return hasNextRound; }
    public String getReason() { return reason; }

    @Override
    public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
