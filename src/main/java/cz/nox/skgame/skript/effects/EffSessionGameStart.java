package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.game.GameMapManager;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Start Session Game")
@Description({
        "Starts the game assigned to a specific session.",
        "",
        "Only works if the session is in STOPPED state and the session's map supports the MiniGame.",
        "Triggers a GameStartEvent.",
        "",
        "Supports: EXECUTE only."
})
@Examples({
        "start game of {_session}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSessionGameStart extends Effect {
    private static final GameMapManager mapManager = GameMapManager.getInstance();
    private Expression<Session> session;

    static {
        Skript.registerEffect(EffSessionGameStart.class,
                "start game of %session%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Session>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Session session = this.session.getSingle(event);
        if (session == null) return;
        MiniGame miniGame = session.getMiniGame();
        GameMap gameMap = session.getGameMap();
        if (miniGame == null || gameMap == null) return;
        if (session.getState() != SessionState.STOPPED) return;
        session.setState(SessionState.STARTED);
        GameStartEvent newEvent = new GameStartEvent(session, miniGame, gameMap);
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
