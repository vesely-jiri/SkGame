package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Total Rounds")
@Description({
        "The total number of rounds configured for a session.",
        "Minimum value is 1. When all rounds complete, the game ends normally.",
        "",
        "Supports: GET / SET."
})
@Examples({
        "set rounds of {_session} to 3",
        "broadcast \"Playing %rounds of {_session}% rounds\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprSessionTotalRounds extends SimplePropertyExpression<Session, Number> {

    static {
        register(ExprSessionTotalRounds.class, Number.class, "[session] rounds", "session");
    }

    @Override
    public @Nullable Number convert(Session session) {
        return session.getTotalRounds();
    }

    @Override
    public @Nullable Class<? extends Number>[] acceptChange(Changer.ChangeMode mode) {
        return mode == Changer.ChangeMode.SET ? CollectionUtils.array(Number.class) : null;
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Session session = getExpr().getSingle(event);
        if (session == null || delta == null || delta[0] == null) return;
        session.setTotalRounds(((Number) delta[0]).intValue());
    }

    @Override
    protected String getPropertyName() {
        return "rounds";
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }
}
