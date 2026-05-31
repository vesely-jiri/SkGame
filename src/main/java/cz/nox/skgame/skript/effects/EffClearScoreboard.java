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
import cz.nox.skgame.core.scoreboard.ScoreboardService;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Scoreboard - Clear")
@Description({
        "Removes the sidebar scoreboard from a session and restores each member's previous board.",
        "No-op if the session has no board."
})
@Examples({
        "clear scoreboard of event-session"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffClearScoreboard extends Effect {

    private Expression<Session> sessionExpr;

    static {
        Skript.registerEffect(EffClearScoreboard.class,
                "clear scoreboard [of] %session%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        sessionExpr = (Expression<Session>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event e) {
        Session session = sessionExpr.getSingle(e);
        if (session == null) return;
        ScoreboardService.getInstance().disposeIfPresent(session);
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "clear scoreboard of " + sessionExpr.toString(e, debug);
    }
}
