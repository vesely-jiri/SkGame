package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.SessionReadOnly;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprSessionId extends SimplePropertyExpression<SessionReadOnly, String> {

    private static final SessionManager sessionManager = SessionManager.getInstance();

    static {
        register(ExprSessionId.class, String.class,
                "id","session"
        );
    }

    @Override
    public @Nullable String convert(SessionReadOnly session) {
        return session.getId();
    }

// TODO - Is this necessary?
//    @Override
//    protected String[] get(Event event, SessionReadOnly[] source) {
//        SessionReadOnly session = getExpr().getSingle(event);
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
