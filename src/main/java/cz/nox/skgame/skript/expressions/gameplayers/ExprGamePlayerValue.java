package cz.nox.skgame.skript.expressions.gameplayers;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.KeyProviderExpression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.GamePlayer;
import cz.nox.skgame.core.game.PlayerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
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
public class ExprGamePlayerValue extends SimpleExpression<Object> implements KeyProviderExpression<Object> {
    private static final PlayerManager playerManager = PlayerManager.getInstance();
    private Expression<String> key;
    private Expression<Player> player;

    private int pattern;
    private int mark;
    private boolean isTemporary;
    private boolean isList;

    static {
        Skript.registerExpression(ExprGamePlayerValue.class, Object.class, ExpressionType.COMBINED,
                "[temp:temp[orary]] [player] value[list:s] %string% of %player%",
                "[all] [temp:temp[orary]] [player] values of %player%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.pattern = pattern;
        if (pattern == 0) {
            key = (Expression<String>) exprs[0];
            player = (Expression<Player>) exprs[1];
        } else {
            player = (Expression<Player>) exprs[0];
        }
        isTemporary = parseResult.hasTag("temp");
        isList = parseResult.hasTag("list");
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event e) {
        if (player == null) return null;
        Player p = player.getSingle(e);
        if (p == null) return null;
        GamePlayer gamePlayer = playerManager.getPlayer(p);
        if (gamePlayer == null) return null;
        switch (pattern) {
            case 0:
                String k = key.getSingle(e);
                if (k == null) return null;
                Object o = gamePlayer.getValue(k,isTemporary);
                if (o == null) return null;
                if (o.getClass().isArray()) {
                    return (Object[]) o;
                } else {
                    return CollectionUtils.array(o);
                }
            case 1: return gamePlayer.getValues(isTemporary);
            default: return null;
            }
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> {
                if (isList) yield CollectionUtils.array(Object[].class);
                yield CollectionUtils.array(Object.class);
            }
            case DELETE, RESET        -> CollectionUtils.array();
            default                   -> null;
        };
    }

    @Override
    public void change(Event e, Object @Nullable [] delta, Changer.ChangeMode mode) {
        if (player == null) return;
        Player p = player.getSingle(e);
        if (p == null) return;
        GamePlayer gamePlayer = playerManager.getPlayer(p);
        if (gamePlayer == null) return;
        switch (mode) {
            case SET -> {
                String k = key.getSingle(e);
                if (delta == null || delta[0] == null || k == null) return;
                if (isList) {
                    gamePlayer.setValue(k, delta, isTemporary);
                } else {
                    gamePlayer.setValue(k, delta[0], isTemporary);
                }
            }
            case DELETE, RESET -> {
                if (pattern == 0) {
                    String k = key.getSingle(e);
                    if (k == null) return;
                    gamePlayer.removeValue(k,isTemporary);
                } else {
                    gamePlayer.removeValues(isTemporary);
                }
            }
        }
    }

    @Override
    public @NotNull String @NotNull [] getArrayKeys(Event e) throws IllegalStateException {
        Player p = player.getSingle(e);
        assert p != null;
        return playerManager.getPlayer(p).getKeys(isTemporary);
    }

    @Override
    public boolean canReturnKeys() {
        return (pattern == 1);
    }

    @Override
    public boolean isLoopOf(String input) {
        return (input.matches("key|index") && pattern == 1);
    }

    @Override
    public boolean isIndexLoop(String input) {
        return (input.matches("key|index") && pattern == 1);
    }

    @Override
    public boolean isSingle() {
        return !isList && pattern == 0;
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        if (pattern == 0) {
            return "player value"
                    + ((isList) ? "s " : " ")
                    + key.toString(e, b)
                    + " of player " + player.toString(e, b);
        } else {
            return "player "
                    + (mark == 0 ? "keys" : "values")
                    + " of player " + player.toString(e, b);
        }
    }
}
