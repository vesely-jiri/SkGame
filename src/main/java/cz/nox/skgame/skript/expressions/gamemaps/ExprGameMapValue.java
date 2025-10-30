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
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprGameMapValue extends SimpleExpression<Object> {
    private Expression<String> key;
    private Expression<GameMap> gameMap;
    private int pattern;
    private int mark;

    static {
        Skript.registerExpression(ExprGameMapValue.class, Object.class, ExpressionType.COMBINED,
                "[[game]map] value %string% of %gamemap%",
                "[all] (keys|1:values) of %gamemap%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.pattern = pattern;
        if (pattern == 0) {
            this.key = (Expression<String>) exprs[0];
            this.gameMap = (Expression<GameMap>) exprs[1];
        } else {
            this.gameMap = (Expression<GameMap>) exprs[0];
            this.mark = parseResult.mark;
        }
        return true;
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
    protected @Nullable Object[] get(Event event) {
        GameMap map = this.gameMap.getSingle(event);
        if (map == null) return null;

        Object o;
        if (this.pattern == 0) {
            String k = this.key.getSingle(event);
            if (k == null) return null;
            o = map.getValue(k);
        } else {
            o = map.getValues();
        }

        if (o instanceof Object[]) {
            return (Object[]) o;
        } else {
            return CollectionUtils.array(o);
        }
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        GameMap map = this.gameMap.getSingle(event);
        String k = this.key.getSingle(event);
        if (map == null || k == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                Object o = delta[0];
                map.setValue(k,o);
            }
            case RESET, DELETE -> map.setValue(k,null);
        }
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public boolean isSingle() {
        return this.pattern == 0;
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        if (this.pattern == 0) {
            return "map value " + this.key.toString(e, b)
                    + " of map " + this.gameMap.toString(e, b);
        } else {
            return "map "
                    + ((this.mark == 0) ? "keys" : "values")
                    + " of map"
                    + this.gameMap.toString(e,b);
        }
    }
}
