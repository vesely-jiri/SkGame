package cz.nox.skgame.skript.expressions.gamemaps;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.GameMap;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Name("GameMap - Value")
@Description({
        "Represents a custom key-value pair stored in a GameMap.",
        "",
        "You can retrieve, set, reset, or delete values associated with a specific key in a GameMap.",
        "Supports fetching all keys or all values of a GameMap.",
        "",
        "Supports: GET / SET / RESET / DELETE."
})
@Examples({
        "set {_map} to gamemap with id \"arena_battle\"",
        "set value \"author\" of {_map} to name of player",
        "broadcast value \"author\" of {_map}",
        "",
        "loop values of {_map}:",
        "    broadcast \"key: %loop-key% or index: %loop-index%\"",
        "    broadcast \"value: %loop-value%\"",
        "",
        "reset value \"author\" of {_map}",
        "delete values of {_map}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprGameMapValue extends SimpleExpression<Object> implements KeyProviderExpression<Object> {
    private Expression<String> key;
    private Expression<GameMap> gameMap;
    private int pattern;
    private int mark;
    private boolean isList;

    static {
        Skript.registerExpression(ExprGameMapValue.class, Object.class, ExpressionType.COMBINED,
                "[[game]map] value[list:s] %string% of %gamemap%",
                "[all] [[game]map] values of %gamemap%"
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
        }
        this.isList = parseResult.hasTag("list");
        return true;
    }


    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET           -> {
                if (isList) yield CollectionUtils.array(Object[].class);
                yield CollectionUtils.array(Object.class);
            }
            case RESET, DELETE -> CollectionUtils.array();
            default            -> null;
        };
    }

    @Override
    protected @Nullable Object[] get(Event event) {
        GameMap map = this.gameMap.getSingle(event);
        if (map == null) return null;
        switch (pattern) {
            case 0:
            Object o;
                String k = this.key.getSingle(event);
                if (k == null) return null;
                o = map.getValue(k);
                if (o == null) return null;
                if (o.getClass().isArray()) {
                    return ((Object[]) o);
                } else {
                    return CollectionUtils.array(o);
                }
            case 1: return map.getValues();
            default: return null;
        }
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        GameMap map = this.gameMap.getSingle(event);
        if (map == null) return;
        switch (mode) {
            case SET -> {
                String k = this.key.getSingle(event);
                if (delta == null || delta[0] == null || k == null) return;
                if (pattern == 1) return;
                Object o = delta[0];
                map.setValue(k,o);
            }
            case RESET, DELETE -> {
                if (pattern == 0) {
                    String k = this.key.getSingle(event);
                    if (k == null) return;
                    map.removeValue(k);
                } else {
                    map.removeValues();
                }
            }
        }
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public boolean isLoopOf(String input) {
        if (input.equals("key") || input.equals("index")) {
            return true;
        }
        return super.isLoopOf(input);
    }

    @Override
    public boolean isIndexLoop(String input) {
        if (input.equals("key") || input.equals("index")) {
            return true;
        }
        return KeyProviderExpression.super.isIndexLoop(input);
    }

    @Override
    public @NotNull String @NotNull [] getArrayKeys(Event e) throws IllegalStateException {
        GameMap map = this.gameMap.getSingle(e);
        assert map != null;
        return map.getKeys();
    }

    @Override
    public boolean canReturnKeys() {
        return (pattern == 1);
    }

    @Override
    public boolean isSingle() {
        return (!isList) && (this.pattern == 0);
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
