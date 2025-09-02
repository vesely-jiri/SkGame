package cz.nox.skgame.skript.expressions.gamemaps;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.core.game.GameMapManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprGameMapAll extends SimpleExpression<GameMap> {
    private static final GameMapManager gameMapManager = GameMapManager.getInstance();

    static {
        Skript.registerExpression(ExprGameMapAll.class, GameMap.class, ExpressionType.SIMPLE,
                "[all] [game]maps"
        );
    }

    @Override
    public boolean init(Expression<?>[] expressions, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    protected GameMap @Nullable [] get(Event event) {
        return gameMapManager.getAllGameMaps();
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
    public String toString(@Nullable Event event, boolean b) {
        return "all gamemaps";
    }
}
