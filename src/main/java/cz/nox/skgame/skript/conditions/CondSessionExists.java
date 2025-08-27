package cz.nox.skgame.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class CondSessionExists extends Condition {
    private static final SessionManager sessionManager = SessionManager.getInstance();
    private Expression<Session> session;

    static {
        Skript.registerCondition(CondSessionExists.class,
                "%sessions% exists"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Session>) exprs[0];
        return true;
    }

    @Override
    public boolean check(Event event) {
        Session[] sessions = this.session.getArray(event);
        if (sessions == null || sessions.length == 0) return false;
        for (Session session : sessions) {
            if (sessionManager.getSessionById(session.getId()) == null) return false;
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return session.getSingle(event) + " does" + (b ? " " : " not ") + "exists";
    }
}
