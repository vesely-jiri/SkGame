package cz.nox.skgame.skript.expressions.sessions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
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
        "Supports: GET / SET / RESET / DELETE."
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

public class ExprSessionValue extends SimpleExpression<Object> {
    private Expression<String> key;
    private Expression<Session> session;

    private int pattern;
    private int mark;
    private boolean isTemporary;

    static {
        Skript.registerExpression(ExprSessionValue.class, Object.class, ExpressionType.COMBINED,
                "[temp:temp[orary]] [session] value[s] %string% of %session%",
                "[all] [temp:temp[orary]] [session] (keys|1:values) of %session%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.pattern = pattern;
        if (pattern == 0) {
            this.key = (Expression<String>) exprs[0];
            this.session = (Expression<Session>) exprs[1];
        } else {
            this.session = (Expression<Session>) exprs[0];
        }
        this.mark = parseResult.mark;
        this.isTemporary = parseResult.hasTag("temp");
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        Session session = this.session.getSingle(event);
        if (session == null) return null;
        switch (pattern) {
            case 0 -> { //single
                Object object = session.getValue(this.key.getSingle(event),this.isTemporary);
                return CollectionUtils.array(object);
            }
            case 1 -> { //all
                if (this.mark == 0) { // keys
                    return CollectionUtils.array(session.getKeys(this.isTemporary));
                } else { // values
                    return CollectionUtils.array(session.getValues(this.isTemporary));
                }
            }
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET, DELETE, RESET -> CollectionUtils.array(Object.class);
            default                 -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Session session = this.session.getSingle(event);
        if (session == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                session.setValue(this.key.getSingle(event),delta[0],this.isTemporary);
            }
            case DELETE, RESET -> {
                if (this.mark == 0) {
                    session.removeValue(this.key.getSingle(event), this.isTemporary);
                } else {
                    session.removeValues(this.isTemporary);
                }
            }
        }
    }

    @Override
    public boolean isSingle() {
        return this.pattern == 0;
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        if (this.pattern == 0) {
            return "session value " + this.key.toString(e,b)
                + "of session " + this.session.toString(e,b);
        } else {
            return (this.isTemporary) ? "temporary " : null
                    + "session "
                    + ((this.mark == 0) ? "keys" : "values")
                    + " of session "
                    + this.session.toString(e,b);
        }
    }
}
