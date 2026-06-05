package cz.nox.skgame.skript.expressions.sessions;

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
import cz.nox.skgame.api.game.model.Session;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - Value")
@Description({
        "Represents a custom value stored in a session, optionally marked as temporary.",
        "You can use it to store or retrieve arbitrary key-value pairs tied to a specific session.",
        "",
        "Temporary values exist only during the session’s lifetime and are not persisted.",
        "Supports both individual key access and full listing of keys or values.",
        "",
        "Setting this value stores data under a given key.",
        "Deleting or resetting removes a specific value or all values, depending on the expression.",
        "",
        "Supports: GET / SET / ADD / REMOVE / RESET / DELETE."
})
@Examples({
        "set {_session} to session with id \"my_custom_session_uuid\"",
        "",
        "set value \"max_rounds\" of {_session} to 5",
        "broadcast value \"max_rounds\" of {_session}",
        "",
        "set temporary value \"deaths\" of {_session} to 0",
        "",
        "loop values of {_session}:",
        "    broadcast \"value: %loop-value%\"",
        "    broadcast \"key: %loop-key% or index: %loop-index%\" ",
        "",
        "delete value \"deaths\" of {_session}",
        "reset all temporary values of {_session}"
})
@Since("1.0.0")

public class ExprSessionValue extends SimpleExpression<Object> implements KeyProviderExpression<Object> {
    private Expression<String> key;
    private Expression<Object> session;

    private int pattern;
    private int mark;
    private boolean isTemporary;
    private boolean isList;

    static {
        // COMBINED: key %string% param + optional temp tag prevent pure property classification
        Skript.registerExpression(ExprSessionValue.class, Object.class, ExpressionType.COMBINED,
                "[temp:temp[orary]] [session] value[list:s] %string% of %object%",
                "[all] [temp:temp[orary]] [session] values of %object%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.pattern = pattern;
        if (pattern == 0) {
            key = (Expression<String>) exprs[0];
            session = (Expression<Object>) exprs[1];
        } else {
            session = (Expression<Object>) exprs[0];
        }
        mark = parseResult.mark;
        isTemporary = parseResult.hasTag("temp");
        isList = parseResult.hasTag("list");
        return true;
    }

    @Override
    protected @Nullable Object[] get(Event e) {
        Object raw = session.getSingle(e);
        if (!(raw instanceof Session s)) return null;
        switch (pattern) {
            case 0:
                String k = key.getSingle(e);
                if (k == null) return null;
                Object o = s.getValue(k,isTemporary);
                if (o == null) return null;
                if (o.getClass().isArray()) {
                    return (Object[]) o;
                } else {
                    return CollectionUtils.array(o);
                }
            case 1:  return s.getValues(isTemporary);
            default: return null;
        }
    }

    @Override
    public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> {
                if (isList) yield CollectionUtils.array(Object[].class);
                yield CollectionUtils.array(Object.class);
            }
            case ADD, REMOVE   -> CollectionUtils.array(Number.class);
            case DELETE, RESET -> CollectionUtils.array();
            default            -> null;
        };
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Object raw = session.getSingle(e);
        if (!(raw instanceof Session s)) return;
        switch (mode) {
            case SET -> {
                String k = key.getSingle(e);
                if (delta == null || delta[0] == null || k == null) return;
                if (isList) {
                    s.setValue(k, delta, isTemporary);
                } else {
                    s.setValue(k, delta[0], isTemporary);
                }
            }
            case ADD -> {
                String k = key.getSingle(e);
                if (delta == null || delta[0] == null || k == null) return;
                Object current = s.getValue(k, isTemporary);
                if (delta[0] instanceof Number dn) {
                    double cur = (current instanceof Number cn) ? cn.doubleValue() : 0.0;
                    s.setValue(k, cur + dn.doubleValue(), isTemporary);
                } else {
                    Object[] arr = current instanceof Object[] a ? a : (current != null ? new Object[]{current} : new Object[0]);
                    Object[] merged = Arrays.copyOf(arr, arr.length + 1);
                    merged[arr.length] = delta[0];
                    s.setValue(k, merged, isTemporary);
                }
            }
            case REMOVE -> {
                String k = key.getSingle(e);
                if (delta == null || delta[0] == null || k == null) return;
                Object current = s.getValue(k, isTemporary);
                if (delta[0] instanceof Number dn) {
                    double cur = (current instanceof Number cn) ? cn.doubleValue() : 0.0;
                    s.setValue(k, cur - dn.doubleValue(), isTemporary);
                } else {
                    Object[] arr = current instanceof Object[] a ? a : (current != null ? new Object[]{current} : new Object[0]);
                    List<Object> list = new ArrayList<>(Arrays.asList(arr));
                    list.remove(delta[0]);
                    s.setValue(k, list.toArray(), isTemporary);
                }
            }
            case DELETE, RESET -> {
                if (pattern == 0) {
                    String k = key.getSingle(e);
                    if (k == null) return;
                    s.removeValue(k, isTemporary);
                } else {
                    s.removeValues(isTemporary);
                }
            }
        }
    }

    @Override
    public @NotNull String[] getArrayKeys(Event e) throws IllegalStateException {
        Object raw = session.getSingle(e);
        if (!(raw instanceof Session s)) return new String[0];
        return s.getKeys(isTemporary);
    }

    @Override
    public boolean canReturnKeys() {
        return (pattern == 1);
    }

    @Override
    public boolean isLoopOf(String input) {
        return (input.matches("key|index") && pattern == 1);
    }

    @Override
    public boolean isIndexLoop(String input) {
        return (input.matches("key|index") && pattern == 1);
    }

    @Override
    public boolean isSingle() {
        return (!isList) && (pattern == 0);
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        if (pattern == 0) {
            return (isTemporary) ? "temporary " : null
                    + "session value"
                    + ((isList) ? "s " : " ")
                    + key.toString(e,b)
                    + " of session " + session.toString(e,b);
        } else {
            return (isTemporary) ? "temporary " : null
                    + "session "
                    + ((mark == 0) ? "keys" : "values")
                    + " of session "
                    + session.toString(e,b);
        }
    }
}
