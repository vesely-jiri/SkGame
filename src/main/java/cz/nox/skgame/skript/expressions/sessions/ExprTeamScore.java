package cz.nox.skgame.skript.expressions.sessions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
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
import cz.nox.skgame.core.util.SessionKeys;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Team Score")
@Description({
        "Gets or changes a team's score in a session.",
        "",
        "Score is stored as an integer under the reserved plugin key 'skgame.team.score.<teamId>'.",
        "It is a temporary session value — auto-reset to 0 at the start of each game/round.",
        "Fractional values are truncated. No floor clamp: negative scores are allowed.",
        "No event is fired on change (use ExprPlayerScore + EvtScoreChange for per-player events).",
        "",
        "Supports: GET / SET / ADD / REMOVE / DELETE."
})
@Examples({
        "# Reset team scores at game start",
        "on \"koth\" game start:",
        "    set team score of \"red\" in event-session to 0",
        "    set team score of \"blue\" in event-session to 0",
        "",
        "# Increment team score and check win condition",
        "add 1 to team score of \"red\" in event-session",
        "if team score of \"red\" in event-session >= 100:",
        "    stop game of event-session with reason \"red_win\"",
        "",
        "# Broadcast scores",
        "broadcast \"Red: %team score of \"red\" in event-session% | Blue: %team score of \"blue\" in event-session%\"",
        "",
        "# Clean up",
        "delete team score of \"red\" in event-session",
        "delete team score of \"blue\" in event-session"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprTeamScore extends SimpleExpression<Number> {

    private static final String KEY_PREFIX = SessionKeys.TEAM_SCORE_PREFIX;

    private Expression<String> teamIdExpr;
    private Expression<Object> sessionExpr;

    static {
        // COMBINED: team id %string% + session %object% are two params, not property-of-type
        Skript.registerExpression(ExprTeamScore.class, Number.class, ExpressionType.COMBINED,
                "[skgame] team score of %string% in %object%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        teamIdExpr  = (Expression<String>) exprs[0];
        sessionExpr = (Expression<Object>) exprs[1];
        return true;
    }

    @Override
    protected @Nullable Number[] get(Event e) {
        String teamId = teamIdExpr.getSingle(e);
        if (teamId == null) return null;
        if (!(sessionExpr.getSingle(e) instanceof Session session)) return null;
        return new Number[]{scoreOf(session, teamId)};
    }

    @Override
    public @Nullable Class<?>[] acceptChange(ChangeMode mode) {
        return switch (mode) {
            case SET, ADD, REMOVE -> CollectionUtils.array(Number.class);
            case DELETE, RESET    -> CollectionUtils.array();
            default               -> null;
        };
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, ChangeMode mode) {
        String teamId = teamIdExpr.getSingle(e);
        if (teamId == null) return;
        if (!(sessionExpr.getSingle(e) instanceof Session session)) return;

        String key = KEY_PREFIX + teamId;

        if (mode == ChangeMode.DELETE || mode == ChangeMode.RESET) {
            session.removeValue(key, true);
            return;
        }

        if (delta == null || delta[0] == null) return;
        int old = scoreOf(session, teamId);
        int amount = ((Number) delta[0]).intValue();
        int newVal = switch (mode) {
            case SET    -> amount;
            case ADD    -> old + amount;
            case REMOVE -> old - amount;
            default     -> old;
        };
        session.setValue(key, newVal, true);
    }

    private static int scoreOf(Session session, String teamId) {
        Object v = session.getValue(KEY_PREFIX + teamId, true);
        return v instanceof Number n ? n.intValue() : 0;
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<? extends Number> getReturnType() { return Number.class; }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        return "team score of " + teamIdExpr.toString(e, b) + " in " + sessionExpr.toString(e, b);
    }
}
