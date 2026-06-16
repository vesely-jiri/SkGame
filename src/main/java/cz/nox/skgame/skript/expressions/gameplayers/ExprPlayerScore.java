package cz.nox.skgame.skript.expressions.gameplayers;

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
import cz.nox.skgame.api.game.event.PlayerScoreChangeEvent;
import cz.nox.skgame.api.game.model.GamePlayer;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.PlayerManager;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.util.GamePlayerKeys;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Player Score")
@Description({
        "Gets or changes a player's score.",
        "",
        "Score is stored as an integer under the reserved plugin key 'skgame.score'.",
        "It is a temporary per-player value — auto-reset to 0 at the start of each game/round.",
        "Fractional values are truncated (e.g. set to 1.5 stores 1).",
        "No floor clamp: negative scores are allowed.",
        "",
        "PlayerScoreChangeEvent is fired on change when the player is in a session.",
        "",
        "Supports: GET / SET / ADD / REMOVE / DELETE."
})
@Examples({
        "set score of event-player to 0",
        "add 1 to score of event-player",
        "remove 2 from score of event-player",
        "broadcast \"Score: %score of event-player%\"",
        "delete score of event-player"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprPlayerScore extends SimpleExpression<Number> {

    private static final String SCORE_KEY = GamePlayerKeys.SCORE;
    private static final PlayerManager playerManager = PlayerManager.getInstance();

    private Expression<Player> playerExpr;

    static {
        // PROPERTY: single type param, qualifies as PROPERTY classification
        Skript.registerExpression(ExprPlayerScore.class, Number.class, ExpressionType.PROPERTY,
                "[skgame] score of %player%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        playerExpr = (Expression<Player>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable Number[] get(Event e) {
        Player p = playerExpr.getSingle(e);
        if (p == null) return null;
        GamePlayer gp = playerManager.getPlayer(p);
        if (gp == null) return null;
        return new Number[]{scoreOf(gp)};
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
        Player p = playerExpr.getSingle(e);
        if (p == null) return;
        GamePlayer gp = playerManager.getPlayer(p);
        if (gp == null) return;

        Session session = SessionManager.getInstance().getSession(p);
        int old = scoreOf(gp);

        if (mode == ChangeMode.DELETE || mode == ChangeMode.RESET) {
            gp.removeValue(SCORE_KEY);
            if (session != null)
                Bukkit.getPluginManager().callEvent(new PlayerScoreChangeEvent(session, p, old, 0));
            return;
        }

        if (delta == null || delta[0] == null) return;
        int amount = ((Number) delta[0]).intValue();
        int newVal = switch (mode) {
            case SET    -> amount;
            case ADD    -> old + amount;
            case REMOVE -> old - amount;
            default     -> old;
        };
        gp.setValue(SCORE_KEY, newVal);
        if (session != null)
            Bukkit.getPluginManager().callEvent(new PlayerScoreChangeEvent(session, p, old, newVal));
    }

    private static int scoreOf(GamePlayer gp) {
        Object v = gp.getValue(SCORE_KEY);
        return v instanceof Number n ? n.intValue() : 0;
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<? extends Number> getReturnType() { return Number.class; }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        return "score of " + playerExpr.toString(e, b);
    }
}
