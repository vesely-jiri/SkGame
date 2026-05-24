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
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.statistics.LeaderboardEntry;
import cz.nox.skgame.core.storage.GameResultsRepository;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
@Name("Statistics - Top Players")
@Description({
        "Returns the top N players for a minigame, ordered by the chosen sort mode.",
        "Sort modes: wins (default), plays, win rate.",
        "Win rate mode supports an optional minimum play count filter.",
        "Requires the database module to be enabled.",
        "",
        "Supports: GET.",
        "",
        "TODO: if leaderboard tables grow significantly, switch to async with cached snapshots."
})
@Examples({
        "set {_top::*} to top 10 players in minigame \"koth\"",
        "set {_top::*} to top 10 players in minigame \"koth\" by wins",
        "set {_top::*} to top 5 players in minigame \"koth\" by plays",
        "set {_top::*} to top 10 players in minigame \"koth\" by win rate",
        "set {_top::*} to top 10 players in minigame \"koth\" by win rate with at least 5 plays"
})
@Since("1.0.0")
public class ExprTopPlayers extends SimpleExpression<OfflinePlayer> {

    // 0=default(wins), 1=wins explicit, 2=plays, 3=win rate, 4=win rate+minPlays
    private int pattern;
    private Expression<Number> limit;
    private Expression<Number> minPlays;
    private Expression<MiniGame> minigame;

    static {
        Skript.registerExpression(ExprTopPlayers.class, OfflinePlayer.class, ExpressionType.COMBINED,
                "top %number% players in [minigame] %minigame%",
                "top %number% players in [minigame] %minigame% by wins",
                "top %number% players in [minigame] %minigame% by plays",
                "top %number% players in [minigame] %minigame% by win[ ]rate",
                "top %number% players in [minigame] %minigame% by win[ ]rate with at least %number% plays"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        pattern = matchedPattern;
        limit = (Expression<Number>) exprs[0];
        minigame = (Expression<MiniGame>) exprs[1];
        if (pattern == 4) {
            minPlays = (Expression<Number>) exprs[2];
        }
        return true;
    }

    @Override
    protected @Nullable OfflinePlayer[] get(Event event) {
        Number lim = limit.getSingle(event);
        MiniGame mg = minigame.getSingle(event);
        if (lim == null || mg == null) return new OfflinePlayer[0];
        int n = Math.max(1, lim.intValue());
        String mgId = mg.getId();

        GameResultsRepository repo = GameResultsRepository.getInstance();
        List<LeaderboardEntry> entries = switch (pattern) {
            case 2 -> repo.getTopPlayersByPlays(mgId, n);
            case 3 -> repo.getTopPlayersByWinRate(mgId, n, 1);
            case 4 -> {
                Number mp = minPlays != null ? minPlays.getSingle(event) : null;
                yield repo.getTopPlayersByWinRate(mgId, n, mp != null ? mp.intValue() : 1);
            }
            default -> repo.getTopPlayersByWins(mgId, n); // 0 and 1
        };

        return entries.stream()
                .map(e -> Bukkit.getOfflinePlayer(e.playerUuid()))
                .toArray(OfflinePlayer[]::new);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends OfflinePlayer> getReturnType() {
        return OfflinePlayer.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String sortStr = switch (pattern) {
            case 2 -> " by plays";
            case 3 -> " by win rate";
            case 4 -> " by win rate with at least " + (minPlays != null ? minPlays.toString(event, debug) : "1") + " plays";
            default -> "";
        };
        return "top " + limit.toString(event, debug) + " players in minigame "
                + minigame.toString(event, debug) + sortStr;
    }
}
