package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.core.game.GameMapManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class EffUnregisterMap extends Effect {

    private static final GameMapManager gameMapManager = GameMapManager.getInstance();
    private Expression<GameMap> gameMap;

    static {
        Skript.registerEffect(EffUnregisterMap.class,
                "unregister %gamemap%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.gameMap = (Expression<GameMap>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        GameMap[] gameMaps = this.gameMap.getArray(event);
        for (GameMap gm : gameMaps) {
            if (gm == null) continue;
            if (gameMapManager.isMapRegistered(gm.getId())) {
                gameMapManager.unregisterGameMap(gm.getId());
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "unregister map " + gameMap.toString(event,b);
    }
}
