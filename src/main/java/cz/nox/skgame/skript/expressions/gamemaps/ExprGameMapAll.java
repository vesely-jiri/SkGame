package cz.nox.skgame.skript.expressions.gamemaps;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.type.GameMapFilter;
import cz.nox.skgame.core.game.GameMapManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("GameMap - All")
@Description({
        "Returns all GameMaps or filtered by their claimed state.",
        "",
        "If no filter is specified, all registered GameMaps are returned.",
        "If 'available' is used, only unclaimed maps are returned.",
        "If 'claimed' (or 'taken') is used, only maps currently in use are returned.",
        "",
        "Supports: GET only."
})
@Examples({
        "loop available gamemaps:",
        "    broadcast \"Available map: %id of loop-gamemap%\"",
        "",
        "loop claimed gamemaps:",
        "    broadcast \"Claimed map: %id of loop-gamemap%\"",
        "",
        "loop gamemaps:",
        "    broadcast \"Map: %id of loop-gamemap%\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprGameMapAll extends SimpleExpression<GameMap> {

    private static final GameMapManager manager = GameMapManager.getInstance();
    private int mark;

    static {
        Skript.registerExpression(ExprGameMapAll.class, GameMap.class, ExpressionType.SIMPLE,
                "[all] [(:available|1:(taken|claimed))] [game]maps");
    }

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.mark = parseResult.mark;
        return true;
    }

    @Override
    protected GameMap @Nullable [] get(Event event) {
        return switch (mark) {
            case 0 ->  manager.getGameMaps(GameMapFilter.AVAILABLE);
            case 1 -> manager.getGameMaps(GameMapFilter.CLAIMED);
            default -> manager.getGameMaps();
        };
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends GameMap> getReturnType() {
        return GameMap.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean d) {
        return switch (mark) {
            case 0 -> "all available gamemaps";
            case 1 -> "all claimed gamemaps";
            default -> "all gamemaps";
        };
    }
}
