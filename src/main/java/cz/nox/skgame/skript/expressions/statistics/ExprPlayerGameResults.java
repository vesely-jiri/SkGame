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
import cz.nox.skgame.core.storage.GameResultsRepository;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
@Name("Statistics - Player Game Results")
@Description({
        "Returns the last N game results for a player, optionally filtered by minigame.",
        "Requires the database module to be enabled.",
        "",
        "Supports: GET."
})
@Examples({
        "set {_results::*} to last 10 game results of player",
        "set {_results::*} to last 10 game results of minigame \"koth\" of player"
})
@Since("1.0.0")
public class ExprPlayerGameResults extends SimpleExpression<GameResult> {

    private Expression<Number> limit;
    private Expression<String> minigameId;
    private Expression<OfflinePlayer> players;
    private int pattern;

    static {
        Skript.registerExpression(ExprPlayerGameResults.class, GameResult.class, ExpressionType.COMBINED,
                "[last] %number% game results of %offlineplayers%",
                "[last] %number% game results of minigame %string% of %offlineplayers%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        pattern = matchedPattern;
        limit = (Expression<Number>) exprs[0];
        if (pattern == 0) {
            players = (Expression<OfflinePlayer>) exprs[1];
        } else {
            minigameId = (Expression<String>) exprs[1];
            players = (Expression<OfflinePlayer>) exprs[2];
        }
        return true;
    }

    @Override
    protected @Nullable GameResult[] get(Event event) {
        Number lim = limit.getSingle(event);
        if (lim == null) return new GameResult[0];
        int n = Math.max(1, lim.intValue());
        String mgId = minigameId != null ? minigameId.getSingle(event) : null;
        OfflinePlayer[] all = players.getAll(event);
        if (all == null) return new GameResult[0];
        List<GameResult> results = new ArrayList<>();
        GameResultsRepository repo = GameResultsRepository.getInstance();
        for (OfflinePlayer p : all) {
            results.addAll(repo.getGameResults(p.getUniqueId(), mgId, n));
        }
        return results.toArray(new GameResult[0]);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends GameResult> getReturnType() {
        return GameResult.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String mgPart = minigameId != null ? " of minigame " + minigameId.toString(event, debug) : "";
        return "last " + limit.toString(event, debug) + " game results" + mgPart
                + " of " + players.toString(event, debug);
    }
}
