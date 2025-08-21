package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.SessionReadOnly;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprSessionState extends SimplePropertyExpression<SessionReadOnly, SessionState> {

    private static final SessionManager sessionManager = SessionManager.getInstance();

    static {
        register(ExprSessionState.class, SessionState.class,
                "state","session");
    }

    @Override
    public @Nullable SessionState convert(SessionReadOnly session) {
        return session.getState();
    }

    @Override
    public Class<? extends SessionState> @Nullable [] acceptChange(ChangeMode mode) {
        return switch (mode) {
            case SET   -> CollectionUtils.array(SessionState.class);
            case RESET -> CollectionUtils.array();
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
        SessionReadOnly session = getExpr().getSingle(event);
        if (session == null) return;
        switch (mode) {
            case ChangeMode.SET -> {
                if (delta == null) return;
                SessionState state = (SessionState) delta[0];
                if (state == null) return;
                sessionManager.setSessionState(session.getId(),state);
            }
            case ChangeMode.RESET -> sessionManager.setSessionState(session.getId(),SessionState.STOPPED);
        }
    }

    @Override
    protected String getPropertyName() {
        return "state";
    }

    @Override
    public Class<? extends SessionState> getReturnType() {
        return SessionState.class;
    }
}
