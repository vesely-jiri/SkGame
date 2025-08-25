package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprSessionName extends SimplePropertyExpression<Session, String> {

    static {
        register(ExprSessionName.class, String.class,
                "name","session");
    }

    @Override
    public @Nullable String convert(Session session) {
        return session.getName();
    }

    @Override
    public Class<? extends String> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET   -> CollectionUtils.array(String.class);
            case RESET -> CollectionUtils.array();
            default    -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Session session = getExpr().getSingle(event);
        if (session == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                String name = (String) delta[0];
                session.setName(name);
            }
            case RESET -> session.setName(null);
        }
    }

    @Override
    protected String getPropertyName() {
        return "name";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }
}
