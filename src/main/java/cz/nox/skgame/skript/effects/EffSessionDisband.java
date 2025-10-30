package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Disband Session")
@Description({
        "Deletes a session.",
})
@Examples({
        "delete session {_session}",
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSessionDisband extends Effect {
    private static final SessionManager sessionManager = SessionManager.getInstance();
    private Expression<Session> session;

    static {
        Skript.registerEffect(EffSessionDisband.class,
                "(delete|disband) [session] %session%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Session>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Session session = this.session.getSingle(event);
        if (session == null) return;
        sessionManager.deleteSession(session.getId());
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "delete session with id " + this.session.toString(event,b);
    }
}
