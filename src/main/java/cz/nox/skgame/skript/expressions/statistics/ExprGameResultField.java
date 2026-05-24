package cz.nox.skgame.skript.expressions.statistics;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.statistics.GameResult;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Statistics - Game Result Field")
@Description({
        "Access individual fields of a game result.",
        "Returns Object — the actual type depends on the field:",
        "  id, start time, end time → number",
        "  minigame id, gamemap id, reason → text",
        "  winner → boolean",
        "",
        "Supports: GET."
})
@Examples({
        "loop {_results::*}:",
        "    if winner of loop-value is true:",
        "        send \"Won %minigame id of loop-value% on %gamemap id of loop-value%\" to player"
})
@Since("1.0.0")
public class ExprGameResultField extends SimpleExpression<Object> {

    // pattern indices
    private static final int ID = 0, MINIGAME_ID = 1, GAMEMAP_ID = 2,
            START_TIME = 3, END_TIME = 4, REASON = 5, WINNER = 6;

    private int pattern;
    private Expression<GameResult> result;

    static {
        Skript.registerExpression(ExprGameResultField.class, Object.class, ExpressionType.PROPERTY,
                "[the] (id|game id) of %gameresult%",
                "[the] minigame [id] of %gameresult%",
                "[the] (gamemap [id]|map [id]) of %gameresult%",
                "[the] start time of %gameresult%",
                "[the] end time of %gameresult%",
                "[the] (reason|stop reason) of %gameresult%",
                "[the] (winner|won) of %gameresult%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        pattern = matchedPattern;
        result = (Expression<GameResult>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable Object[] get(Event event) {
        GameResult r = result.getSingle(event);
        if (r == null) return null;
        Object value = switch (pattern) {
            case ID         -> r.id();
            case MINIGAME_ID -> r.minigameId();
            case GAMEMAP_ID  -> r.gamemapId();
            case START_TIME  -> r.startTime();
            case END_TIME    -> r.endTime();
            case REASON      -> r.reason();
            case WINNER      -> r.winner();
            default          -> null;
        };
        return value != null ? new Object[]{value} : null;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String field = switch (pattern) {
            case ID         -> "id";
            case MINIGAME_ID -> "minigame id";
            case GAMEMAP_ID  -> "gamemap id";
            case START_TIME  -> "start time";
            case END_TIME    -> "end time";
            case REASON      -> "reason";
            case WINNER      -> "winner";
            default          -> "?";
        };
        return field + " of " + result.toString(event, debug);
    }
}
