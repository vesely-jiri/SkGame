package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprSessionId extends SimplePropertyExpression<Session, String> {

    static {
        register(ExprSessionId.class, String.class,
                "id","session"
        );
    }

    @Override
    public @Nullable String convert(Session session) {
        return session.getId();
    }

//    @Override
//    protected String[] get(Event event, Session[] source) {
//        Session session = getExpr().getSingle(event);
//        if (session == null) return null;
//        return CollectionUtils.array(session.getId());
//    }

    @Override
    protected String getPropertyName() {
        return "id";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }
}
