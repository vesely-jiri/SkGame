package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.GameMode;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.game.GameMapManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class EffSessionGameStart extends Effect {
    private static final GameMapManager mapManager = GameMapManager.getInstance();
    private Expression<Session> session;

    static {
        Skript.registerEffect(EffSessionGameStop.class,
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
        System.out.println("Start of trigger " + this.getClass());
        Session session = this.session.getSingle(event);
        if (session == null) return;

        GameMode gameMode = session.getGameMode();
        GameMap gameMap = session.getGameMap();
        if (gameMode == null || gameMap == null) return;

        if (session.getState() != SessionState.STOPPED) return;
        if (session.getGameMode() == null) return;
        if (session.getGameMap() == null) return;
        if (mapManager.isMapClaimed(session.getGameMap().getId())) return;

        // TODO - If GameMap is not taken
        // TODO - If GameMap is in good condition(all map/gamemode values are properly set)
        // TODO - If other things?

        // TODO - Trigger event that is tied up with gamemode script

        System.out.println("End of trigger " + this.getClass());
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        Session session = this.session.getSingle(event);
        if (session == null) return "Session does not exist";
        GameMode gameMode = session.getGameMode();
        return "start game " + gameMode ;
    }
}
