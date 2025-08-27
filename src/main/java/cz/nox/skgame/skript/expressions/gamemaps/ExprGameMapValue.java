package cz.nox.skgame.skript.expressions.gamemaps;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.GameMode;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprGameMapValue extends SimpleExpression<Object> {
    private Expression<String> key;
    private Expression<GameMap> gameMap;
    private Expression<GameMode> gameMode;

    static {
        Skript.registerExpression(ExprGameMapValue.class, Object.class, ExpressionType.COMBINED,
                "value %string% of %gamemap% (of|from) %gamemode%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.key = (Expression<String>) exprs[0];
        this.gameMap = (Expression<GameMap>) exprs[1];
        this.gameMode = (Expression<GameMode>) exprs[2];
        return true;
    }

    @Override
    protected @Nullable Object [] get(Event event) {
        GameMap map = this.gameMap.getSingle(event);
        GameMode gm = this.gameMode.getSingle(event);
        String k = this.key.getSingle(event);
        if (map == null || gm == null || k == null) return null;
        Object o = map.getGameModeValue(gm.getId(),key.getSingle(event));
        return CollectionUtils.array(o);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET           -> CollectionUtils.array(Object.class);
            case RESET, DELETE -> CollectionUtils.array();
            default            -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        GameMap map = this.gameMap.getSingle(event);
        GameMode gm = this.gameMode.getSingle(event);
        String k = this.key.getSingle(event);
        if (map == null || gm == null || key == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                Object o = delta[0];
                map.setGameModeValue(gm.getId(),k,o);
            }
            case RESET, DELETE -> {
                map.setGameModeValue(gm.getId(),k,null);
            }
        }
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "map value " + this.key.toString(event,b)
                + " of map " + this.gameMap.toString(event,b)
                + " of gamemode " + this.gameMode.toString(event,b);
    }
}
