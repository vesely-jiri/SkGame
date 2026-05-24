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
        "set {_top::*} to top 10 players of minigame \"koth\"",
        "set {_top::*} to top 5 players by plays of minigame \"koth\"",
        "set {_top::*} to top 10 players by win rate with at least 5 plays of minigame \"koth\""
})
@Since("1.0.0")
public class ExprTopPlayers extends SimpleExpression<OfflinePlayer> {

    // 0=WINS, 1=PLAYS, 2=WIN_RATE (no minPlays), 3=WIN_RATE (with minPlays)
    private int pattern;
    private Expression<Number> limit;
    private Expression<Number> minPlays;
    private Expression<String> minigameId;

    static {
        Skript.registerExpression(ExprTopPlayers.class, OfflinePlayer.class, ExpressionType.COMBINED,
                "top %number% players [by wins] of minigame %string%",
                "top %number% players by plays of minigame %string%",
                "top %number% players by win[ ]rate of minigame %string%",
                "top %number% players by win[ ]rate with at least %number% plays of minigame %string%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        pattern = matchedPattern;
        limit = (Expression<Number>) exprs[0];
        if (pattern == 3) {
            minPlays = (Expression<Number>) exprs[1];
            minigameId = (Expression<String>) exprs[2];
        } else {
            minigameId = (Expression<String>) exprs[1];
        }
        return true;
    }

    @Override
    protected @Nullable OfflinePlayer[] get(Event event) {
        Number lim = limit.getSingle(event);
        String mgId = minigameId.getSingle(event);
        if (lim == null || mgId == null) return new OfflinePlayer[0];
        int n = Math.max(1, lim.intValue());

        GameResultsRepository repo = GameResultsRepository.getInstance();
        List<LeaderboardEntry> entries = switch (pattern) {
            case 1 -> repo.getTopPlayersByPlays(mgId, n);
            case 2 -> repo.getTopPlayersByWinRate(mgId, n, 1);
            case 3 -> {
                Number mp = minPlays != null ? minPlays.getSingle(event) : null;
                yield repo.getTopPlayersByWinRate(mgId, n, mp != null ? mp.intValue() : 1);
            }
            default -> repo.getTopPlayersByWins(mgId, n);
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
            case 1 -> " by plays";
            case 2 -> " by win rate";
            case 3 -> " by win rate with at least " + (minPlays != null ? minPlays.toString(event, debug) : "1") + " plays";
            default -> "";
        };
        return "top " + limit.toString(event, debug) + " players" + sortStr
                + " of minigame " + minigameId.toString(event, debug);
    }
}
