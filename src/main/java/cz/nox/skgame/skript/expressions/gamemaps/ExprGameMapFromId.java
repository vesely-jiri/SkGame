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

import java.util.Arrays;

@SuppressWarnings("unused")
public class ExprGameMapFromId extends SimpleExpression<GameMap> {
    private static final GameMapManager gameMapManager = GameMapManager.getInstance();
    private Expression<String> uuids;

    static {
        Skript.registerExpression(ExprGameMapFromId.class, GameMap.class, ExpressionType.COMBINED,
                "[game]map[s] (with|from) [[uu]id[s]] %strings%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.uuids = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable GameMap[] get(Event event) {
        return Arrays.stream(this.uuids.getArray(event))
                .map(gameMapManager::getGameMapById)
                .toArray(GameMap[]::new);
    }

    @Override
    public Class<? extends GameMap> getReturnType() {
        return GameMap.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "gamemap[s] with id " + this.uuids.toString(event,b);
    }
}
