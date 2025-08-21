package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.SessionReadOnly;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprSessionName extends SimplePropertyExpression<SessionReadOnly, String> {

    private static final SessionManager sessionManager = SessionManager.getInstance();

    static {
        register(ExprSessionName.class, String.class,
                "name","session");
    }

    @Override
    public @Nullable String convert(SessionReadOnly session) {
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
        SessionReadOnly session = getExpr().getSingle(event);
        if (session == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                String name = (String) delta[0];
                sessionManager.setSessionName(session.getId(),name);
            }
            case RESET -> sessionManager.setSessionName(session.getId(),null);
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
