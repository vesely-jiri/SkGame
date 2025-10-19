package cz.nox.skgame.skript.expressions.sessions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
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
                session.setValue(this.key.getSingle(event),delta[0],isTemporary);
            }
            case DELETE, RESET -> {
                if (mark == 0) {
                    session.removeValue(this.key.getSingle(event), isTemporary);
                } else {
                    session.removeValues(isTemporary);
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
    public String toString(@Nullable Event event, boolean b) {
        if (pattern == 0) {
            return "session value " + this.key.toString(event,b)
                + "of session " + this.session.toString(event,b);
        } else {
            return "";
        }
    }
}
