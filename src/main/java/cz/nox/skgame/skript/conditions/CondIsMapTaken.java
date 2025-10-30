package cz.nox.skgame.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.core.game.GameMapManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("GameMap - Is Map Taken")
@Description({
        "Checks if a GameMap is currently taken by other session.",
        "",
        "Useful for preventing multiple sessions from trying to use the same map."
})
@Examples({
        "if {_map} is taken:",
        "\tsend \"This map is currently in use!\" to player"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class CondIsMapTaken extends Condition {
    private static final GameMapManager mapManager = GameMapManager.getInstance();
    Expression<GameMap> gameMap;

    static {
        Skript.registerCondition(CondIsMapTaken.class,
                "%gamemap% is (taken|claimed|being used)"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.gameMap = (Expression<GameMap>) exprs[0];
        return true;
    }

    @Override
    public boolean check(Event event) {
        GameMap map = gameMap.getSingle(event);
        if (map == null) return false;
        return mapManager.isMapClaimed(map.getId());
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "Map " + gameMap.getSingle(event) + " is " + (b ? "" : "not ") + "taken";
    }
}
