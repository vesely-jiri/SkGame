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
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Cancel Countdown")
@Description({
        "Cancels an ongoing countdown started by 'start game of %session% in %timespan%'.",
        "Only has effect if the session is in STARTING state.",
        "Sets the session state back to STOPPED.",
})
@Examples({
        "cancel countdown of {_session}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSessionCancelCountdown extends Effect {

    private static final SessionManager sessionManager = SessionManager.getInstance();
    private Expression<Session> session;

    static {
        Skript.registerEffect(EffSessionCancelCountdown.class,
                "cancel countdown of %session%"
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
        if (session.getState() != SessionState.STARTING) return;
        sessionManager.cancelCountdownTask(session.getId());
        session.setState(SessionState.STOPPED);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "cancel countdown of " + this.session.toString(event, b);
    }
}
