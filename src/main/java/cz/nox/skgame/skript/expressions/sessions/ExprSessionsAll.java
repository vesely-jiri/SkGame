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
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - All Sessions")
@Description({
        "Returns a list of all currently active game sessions.",
        "Can also be used to delete or reset all sessions at once.",
        "",
        "Useful for administrative scripts that need to manage or clear all running sessions.",
        "",
        "Supports: GET / RESET / DELETE."
})
@Examples({
        "loop all sessions:",
        "    broadcast \"Session ID: %id of loop-value%\"",
        "",
        "delete all sessions",
        "reset all sessions"
})
@Since("1.0.0")

public class ExprSessionsAll extends SimpleExpression<Session> {
    private static final SessionManager sessionManager = SessionManager.getInstance();

    static {
        Skript.registerExpression(ExprSessionsAll.class, Session.class, ExpressionType.SIMPLE,
                "[all] [game] sessions"
        );
    }

    @Override
    public boolean init(Expression<?>[] expressions, int i, Kleenean kleenean, SkriptParser.ParseResult parse) {
        return true;
    }

    @Override
    protected Session @Nullable [] get(Event event) {
        return sessionManager.getAllSessions();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.RESET || mode == Changer.ChangeMode.DELETE)
            return CollectionUtils.array(Session.class);
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (mode == Changer.ChangeMode.RESET || mode == Changer.ChangeMode.DELETE) {
            for (Session session : sessionManager.getAllSessions()) {
                sessionManager.deleteSession(session.getId());
            }
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Session> getReturnType() {
        return Session.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "all sessions";
    }
}
