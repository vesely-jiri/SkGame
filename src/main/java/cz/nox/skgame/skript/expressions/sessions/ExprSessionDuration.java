package cz.nox.skgame.skript.expressions.sessions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.util.Timespan;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session Duration")
@Description({
        "Returns the elapsed time since the current game round started (from the STARTED state).",
        "Returns null if the session is not in STARTED state (LOBBY, ENDED, etc.).",
        "Resets to zero on each new round.",
})
@Examples({
        "# Timeout check — stop game after 10 minutes",
        "every 30 seconds:",
        "    loop all sessions:",
        "        if state of loop-session is started:",
        "            if session duration of loop-session > 10 minutes:",
        "                stop game of loop-session with reason \"timeout\"",
        "",
        "# Broadcast elapsed time",
        "broadcast \"Session running for %session duration of event-session%\"",
        "",
        "# Use in condition with different units",
        "if duration of event-session >= 5 minutes:",
        "    send actionbar locale \"koth:overtime\" to session players of event-session"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprSessionDuration extends SimpleExpression<Timespan> {

    private Expression<Session> session;

    static {
        Skript.registerExpression(ExprSessionDuration.class, Timespan.class, ExpressionType.PROPERTY,
                "[session] duration of %session%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Session>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable Timespan[] get(Event event) {
        Session s = session.getSingle(event);
        if (s == null) return null;
        Timespan duration = s.getGameDuration();
        return duration != null ? new Timespan[]{duration} : null;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Timespan> getReturnType() {
        return Timespan.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "session duration of " + session.toString(event, debug);
    }
}
