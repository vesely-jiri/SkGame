package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.event.GameStopEvent;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.game.GameMapManager;
import cz.nox.skgame.core.game.PlayerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class EffSessionGameStop extends Effect {
    private static final GameMapManager mapManager = GameMapManager.getInstance();
    private static final PlayerManager playerManager = PlayerManager.getInstance();
    private Expression<Session> session;
    private Expression<String> reason;

    static {
        Skript.registerEffect(EffSessionGameStop.class,
                "stop game of %session% [with reason %string%]"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Session>) exprs[0];
        if (exprs[1] != null) {
            this.reason = (Expression<String>) exprs[1];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        Session session = this.session.getSingle(event);
        if (session == null) return;
        MiniGame miniGame = session.getMiniGame();
        if (miniGame == null) return;
        GameStopEvent newEvent;
        if (this.reason != null) {
            String re = this.reason.getSingle(event);
            newEvent = new GameStopEvent(miniGame, session, re);
        } else {
            newEvent = new GameStopEvent(miniGame, session, "default");
        }
        session.setState(SessionState.STOPPED);
        session.removeValues(true);
        for (Player player : session.getPlayers()) {
            playerManager.getPlayer(player).removeValues(true);
        }
        Bukkit.getPluginManager().callEvent(newEvent);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        Session session = this.session.getSingle(event);
        if (session == null) return "Session does not exist";
        MiniGame miniGame = session.getMiniGame();
        return "start game " + miniGame + " of session with id " + session.getId() ;
    }
}
