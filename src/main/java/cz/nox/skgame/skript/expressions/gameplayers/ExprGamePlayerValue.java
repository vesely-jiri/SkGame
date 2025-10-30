package cz.nox.skgame.skript.expressions.gameplayers;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
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
import cz.nox.skgame.api.game.model.GamePlayer;
import cz.nox.skgame.core.game.PlayerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("GamePlayer - Value")
@Description({
        "Represents a custom value stored for a specific player.",
        "Supports temporary values if 'temporary' tag is used. When game ends, temporary tagged values are deleted.",
        "",
        "Also supports retrieving all keys or all values of a player.",
        "",
        "Supports: GET / SET / RESET / DELETE."
})
@Examples({
        "set temporary value \"score\" of player to 10",
        "broadcast value \"score\" of player",
        "",
        "loop keys of player:",
        "    broadcast \"Key: %loop-value%\"",
        "",
        "delete value \"score\" of player",
        "reset values of player"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprGamePlayerValue extends SimpleExpression<Object> {
    private static final PlayerManager playerManager = PlayerManager.getInstance();
    private Expression<String> key;
    private Expression<Player> players;

    private int pattern;
    private int mark;
    private boolean isTemporary;

    static {
        Skript.registerExpression(ExprGamePlayerValue.class, Object.class, ExpressionType.COMBINED,
                "[temp:temp[orary]] [player] value %string% of %players%",
                "[all] [temp:temp[orary]] [player] (keys|1:values) of %players%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.pattern = pattern;
        if (pattern == 0) {
            this.key = (Expression<String>) exprs[0];
            this.players = (Expression<Player>) exprs[1];
        } else {
            this.players = (Expression<Player>) exprs[0];
        }
        this.mark = parseResult.mark;
        this.isTemporary = parseResult.hasTag("temp");
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        Player player = this.players.getSingle(event);
        String key = this.key.getSingle(event);
        if (player == null) return null;
        GamePlayer gamePlayer = playerManager.getPlayer(player);
        if (gamePlayer == null || key == null) return null;
        switch (pattern) {
            case 0 -> { //Single
                return CollectionUtils.array(gamePlayer.getValue(key,this.isTemporary));
            }
            case 1 -> { //all
                if (this.mark == 0) { //keys
                    return CollectionUtils.array(gamePlayer.getKeys(this.isTemporary));
                } else { //values
                    return CollectionUtils.array(gamePlayer.getValues(this.isTemporary));
                }
            }
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET/*, ADD, REMOVE*/ -> CollectionUtils.array(Object.class);
            case DELETE, RESET    -> CollectionUtils.array();
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Player player = this.players.getSingle(event);
        if (player == null) return;
        GamePlayer gamePlayer = playerManager.getPlayer(player);
        if (gamePlayer == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                gamePlayer.setValue(this.key.getSingle(event),delta[0],this.isTemporary);
            }
            case DELETE, RESET -> {
                if (pattern == 0) {
                    gamePlayer.removeValue(this.key.getSingle(event),this.isTemporary);
                } else {
                    gamePlayer.removeValues(this.isTemporary);
                }
            }
        }
    }

    @Override
    public boolean isSingle() {
        return this.pattern == 0;
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        if (pattern == 0) {
            return "player value "
                    + this.key.toString(e, b)
                    + " of player[s] " + this.players.toString(e, b);
        } else {
            return "player "
                    + (this.mark == 0 ? "keys" : "values")
                    + " of player[s] " + this.players.toString(e, b);
        }
    }
}
