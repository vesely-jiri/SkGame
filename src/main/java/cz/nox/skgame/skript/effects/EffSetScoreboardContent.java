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

import java.util.Arrays;
import java.util.List;

@Name("Scoreboard - Set Content")
@Description({
        "Sets the content lines of a session's sidebar scoreboard.",
        "",
        "Lines are plain pre-formatted strings — resolve variables before calling.",
        "The lines replace the {content} placeholder defined in scoreboard.yml.",
        "Requires the minigame to declare: set minigame value \"scoreboard\" of event-minigame to \"per-session\"",
        "No-op if the session has no board (scoreboard not declared for that minigame)."
})
@Examples({
        "set scoreboard content of event-session to \"&aPoints: 0\"",
        "set scoreboard content of event-session to \"&aKills: %{_kills}%\", \"&7Deaths: %{_deaths}%\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSetScoreboardContent extends Effect {

    private Expression<Session> sessionExpr;
    private Expression<String> linesExpr;

    static {
        Skript.registerEffect(EffSetScoreboardContent.class,
                "set scoreboard content of %session% to %strings%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        sessionExpr = (Expression<Session>) exprs[0];
        linesExpr   = (Expression<String>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event e) {
        Session session = sessionExpr.getSingle(e);
        if (session == null) return;
        String[] raw = linesExpr.getAll(e);
        List<String> lines = raw != null ? Arrays.asList(raw) : List.of();
        ScoreboardService.getInstance().setContent(session, lines);
    }

    @Override
    public String toString(@Nullable Event e, boolean debug) {
        return "set scoreboard content of " + sessionExpr.toString(e, debug)
                + " to " + linesExpr.toString(e, debug);
    }
}
