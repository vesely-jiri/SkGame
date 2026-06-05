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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
        "Supports: GET / SET / ADD / REMOVE / RESET / DELETE."
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
    private Expression<Object> gameMap;
    private int pattern;
    private int mark;
    private boolean isList;

    static {
        // COMBINED: two patterns with different structures; key %string% param prevents pure property classification
        Skript.registerExpression(ExprGameMapValue.class, Object.class, ExpressionType.COMBINED,
                "[the] map value[list:s] %string% of %object%",
                "[all] [the] map values of %object%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.pattern = pattern;
        if (pattern == 0) {
            this.key = (Expression<String>) exprs[0];
            this.gameMap = (Expression<Object>) exprs[1];
        } else {
            this.gameMap = (Expression<Object>) exprs[0];
        }
        this.isList = parseResult.hasTag("list");
        return true;
    }


    @Override
    public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET           -> {
                if (isList) yield CollectionUtils.array(Object[].class);
                yield CollectionUtils.array(Object.class);
            }
            case ADD, REMOVE   -> CollectionUtils.array(Number.class);
            case RESET, DELETE -> CollectionUtils.array();
            default            -> null;
        };
    }

    @Override
    protected @Nullable Object[] get(Event event) {
        Object raw = this.gameMap.getSingle(event);
        if (!(raw instanceof GameMap map)) return null;
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
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Object raw = this.gameMap.getSingle(event);
        if (!(raw instanceof GameMap map)) return;
        switch (mode) {
            case SET -> {
                String k = this.key.getSingle(event);
                if (delta == null || delta[0] == null || k == null) return;
                if (pattern == 1) return;
                Object o = delta[0];
                map.setValue(k,o);
            }
            case ADD -> {
                String k = this.key.getSingle(event);
                if (delta == null || delta[0] == null || k == null) return;
                Object current = map.getValue(k);
                if (delta[0] instanceof Number dn) {
                    double cur = (current instanceof Number cn) ? cn.doubleValue() : 0.0;
                    map.setValue(k, cur + dn.doubleValue());
                } else {
                    Object[] arr = current instanceof Object[] a ? a : (current != null ? new Object[]{current} : new Object[0]);
                    Object[] merged = Arrays.copyOf(arr, arr.length + 1);
                    merged[arr.length] = delta[0];
                    map.setValue(k, merged);
                }
            }
            case REMOVE -> {
                String k = this.key.getSingle(event);
                if (delta == null || delta[0] == null || k == null) return;
                Object current = map.getValue(k);
                if (delta[0] instanceof Number dn) {
                    double cur = (current instanceof Number cn) ? cn.doubleValue() : 0.0;
                    map.setValue(k, cur - dn.doubleValue());
                } else {
                    Object[] arr = current instanceof Object[] a ? a : (current != null ? new Object[]{current} : new Object[0]);
                    List<Object> list = new ArrayList<>(Arrays.asList(arr));
                    list.remove(delta[0]);
                    map.setValue(k, list.toArray());
                }
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
    public @NotNull String[] getArrayKeys(Event e) throws IllegalStateException {
        Object raw = this.gameMap.getSingle(e);
        if (!(raw instanceof GameMap map)) return new String[0];
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
