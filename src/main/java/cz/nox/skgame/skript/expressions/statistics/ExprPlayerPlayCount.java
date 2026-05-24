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
import cz.nox.skgame.core.storage.GameResultsRepository;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Statistics - Player Play Count")
@Description({
        "Returns the number of games played by a player for a given minigame.",
        "Requires the database module to be enabled.",
        "",
        "Supports: GET."
})
@Examples({
        "set {_plays} to play count of minigame \"koth\" of player",
        "send \"You played %play count of minigame \"\"koth\"\" of player% times!\" to player"
})
@Since("1.0.0")
public class ExprPlayerPlayCount extends SimpleExpression<Long> {

    private Expression<String> minigameId;
    private Expression<OfflinePlayer> player;

    static {
        Skript.registerExpression(ExprPlayerPlayCount.class, Long.class, ExpressionType.COMBINED,
                "play count of minigame %string% of %offlineplayer%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        minigameId = (Expression<String>) exprs[0];
        player = (Expression<OfflinePlayer>) exprs[1];
        return true;
    }

    @Override
    protected @Nullable Long[] get(Event event) {
        String mgId = minigameId.getSingle(event);
        OfflinePlayer p = player.getSingle(event);
        if (mgId == null || p == null) return null;
        int plays = GameResultsRepository.getInstance().getPlayCount(p.getUniqueId(), mgId);
        return new Long[]{(long) plays};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Long> getReturnType() {
        return Long.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "play count of minigame " + minigameId.toString(event, debug)
                + " of " + player.toString(event, debug);
    }
}
