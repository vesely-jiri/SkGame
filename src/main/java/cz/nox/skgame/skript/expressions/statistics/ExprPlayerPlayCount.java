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
        "set {_plays} to play count of player in minigame \"koth\"",
        "send \"You played %play count of player in minigame \"\"koth\"\"%times!\" to player"
})
@Since("1.0.0")
public class ExprPlayerPlayCount extends SimpleExpression<Number> {

    private Expression<OfflinePlayer> player;
    private Expression<MiniGame> minigame;

    static {
        Skript.registerExpression(ExprPlayerPlayCount.class, Number.class, ExpressionType.COMBINED,
                "play[s] count of %offlineplayer% in [minigame] %minigame%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        player = (Expression<OfflinePlayer>) exprs[0];
        minigame = (Expression<MiniGame>) exprs[1];
        return true;
    }

    @Override
    protected @Nullable Number[] get(Event event) {
        OfflinePlayer p = player.getSingle(event);
        MiniGame mg = minigame.getSingle(event);
        if (p == null || mg == null) return null;
        int plays = GameResultsRepository.getInstance().getPlayCount(p.getUniqueId(), mg.getId());
        return new Number[]{plays};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "play count of " + player.toString(event, debug)
                + " in minigame " + minigame.toString(event, debug);
    }
}
