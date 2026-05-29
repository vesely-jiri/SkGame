package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import cz.nox.skgame.api.game.model.Session;
import org.jetbrains.annotations.Nullable;

@Name("Session - Current Round")
@Description({
        "The current round number of a session. 0 when no game is active (lobby state).",
        "",
        "Supports: GET only."
})
@Examples({
        "broadcast \"Round %session current round of event-session% of %session rounds of event-session%\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprSessionCurrentRound extends SimplePropertyExpression<Session, Number> {

    static {
        register(ExprSessionCurrentRound.class, Number.class, "[session] current round", "session");
        register(ExprSessionCurrentRound.class, Number.class, "current [session] round", "session");
    }

    @Override
    public @Nullable Number convert(Session session) {
        return session.getCurrentRound();
    }

    @Override
    protected String getPropertyName() {
        return "current round";
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }
}
