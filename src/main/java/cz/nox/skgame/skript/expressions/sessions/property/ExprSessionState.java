package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - State")
@Description({
        "Represents the current state of a game session.",
        "You can retrieve or change the session's state, such as waiting, running, or stopped.",
        "",
        "Setting this value changes the session's active state.",
        "Resetting this value sets the state back to 'STOPPED'.",
        "If state is not STARTED, condition \"%player% is playing\" returns always false",
        "If state is not STOPPED, effect \"start game\" won't start the game => session must be in STOPPED state to be able to start it's minigame",
        "",
        "Supports: GET / SET / RESET."
})
@Examples({
        "set {_session} to session with id \"the_session_id\"",
        "if state of {_session} is RUNNING:",
        "    broadcast \"The session is running!\"",
        "",
        "set state of {_session} to STOPPED",
        "reset state of {_session}"
})
@Since("1.0.0")

public class ExprSessionState extends SimplePropertyExpression<Session, SessionState> {

    static {
        register(ExprSessionState.class, SessionState.class,
                "state","session");
    }

    @Override
    public @Nullable SessionState convert(Session session) {
        return session.getState();
    }

    @Override
    public Class<? extends SessionState> @Nullable [] acceptChange(ChangeMode mode) {
        return switch (mode) {
            case SET, RESET   -> CollectionUtils.array(SessionState.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
        Session session = getExpr().getSingle(event);
        if (session == null) return;
        switch (mode) {
            case ChangeMode.SET -> {
                if (delta == null) return;
                SessionState state = (SessionState) delta[0];
                if (state == null) return;
                session.setState(state);
            }
            case ChangeMode.RESET -> session.setState(SessionState.STOPPED);
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
