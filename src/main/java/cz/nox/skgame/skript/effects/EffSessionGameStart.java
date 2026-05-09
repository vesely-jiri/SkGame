package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.util.Timespan.TimePeriod;
import ch.njol.util.Kleenean;
import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.region.ArenaSlot;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

@Name("Session - Start Session Game")
@Description({
        "Starts the game assigned to a specific session.",
        "Optionally provide a countdown delay — the session enters STARTING state during the delay.",
        "",
        "Only works if the session is in STOPPED state and the session's map supports the MiniGame.",
        "Triggers a GameStartEvent when the game actually begins.",
        "",
        "Supports: EXECUTE only."
})
@Examples({
        "start game of {_session}",
        "start game of {_session} in 10 seconds"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSessionGameStart extends Effect {

    private static final SessionManager sessionManager = SessionManager.getInstance();
    private Expression<Session> session;
    private @Nullable Expression<Timespan> delay;

    static {
        Skript.registerEffect(EffSessionGameStart.class,
                "start game of %session% [in %-timespan%]"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Session>) exprs[0];
        this.delay = (Expression<Timespan>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Session session = this.session.getSingle(event);
        if (session == null) return;
        if (session.getState() != SessionState.STOPPED) return;

        MiniGame miniGame = session.getMiniGame();
        GameMap gameMap = session.getGameMap();
        if (miniGame == null || gameMap == null) return;
        if (!gameMap.supportsMiniGame(miniGame)) return;

        if (this.delay != null) {
            Timespan timespan = this.delay.getSingle(event);
            if (timespan != null) {
                startWithCountdown(session, timespan);
                return;
            }
        }
        startImmediately(session);
    }

    private void startImmediately(Session session) {
        session.setState(SessionState.STARTED);
        GameMap gameMap = session.getGameMap();
        if (gameMap != null && gameMap.hasArenaSlots()) {
            ArenaSlot slot = gameMap.claimSlot(session.getId());
            if (slot != null) {
                session.setClaimedSlot(slot);
                session.setArenaRegion(gameMap.getSlotRegion(slot));
            }
        }
        Bukkit.getPluginManager().callEvent(
                new GameStartEvent(session, session.getMiniGame(), gameMap)
        );
    }

    private void startWithCountdown(Session session, Timespan timespan) {
        session.setState(SessionState.STARTING);
        String sessionId = session.getId();
        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                sessionManager.cancelCountdownTask(sessionId);
                Session current = sessionManager.getSessionById(sessionId);
                if (current == null || current.getState() != SessionState.STARTING) return;
                startImmediately(current);
            }
        }.runTaskLater(SkGame.getInstance(), timespan.getAs(TimePeriod.TICK));
        sessionManager.setCountdownTask(sessionId, task);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "start game of " + this.session.toString(event, b);
    }
}
