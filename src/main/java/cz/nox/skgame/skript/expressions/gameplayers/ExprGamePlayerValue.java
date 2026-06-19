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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import cz.nox.skgame.core.game.PlayerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Name("GamePlayer - Value")
@Description({
        "Represents a custom value stored for a specific player. Values are always temporary — reset when the game ends.",
        "",
        "Also supports retrieving all keys or all values of a player.",
        "",
        "Supports: GET / SET / ADD / REMOVE / RESET / DELETE."
})
@Examples({
        "# GET / SET / ADD / DELETE",
        "set player value \"kills\" of event-player to 0",
        "add 1 to player value \"kills\" of event-player",
        "remove 1 from player value \"kills\" of event-player",
        "broadcast \"%name of event-player% kills: %player value \"kills\" of event-player%\"",
        "delete player value \"kills\" of event-player",
        "",
        "# Loop all values",
        "loop player values of event-player:",
        "    broadcast \"%loop-key%: %loop-value%\"",
        "",
        "# Reset all values at game end",
        "on game stop:",
        "    loop session players of event-session:",
        "        reset player values of loop-player"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprGamePlayerValue extends SimpleExpression<Object> implements KeyProviderExpression<Object> {
    private static final PlayerManager playerManager = PlayerManager.getInstance();
    private Expression<String> key;
    private Expression<Object> player;

    private int pattern;
    private int mark;
    private boolean isList;

    static {
        // COMBINED: key %string% param prevents pure property classification
        Skript.registerExpression(ExprGamePlayerValue.class, Object.class, ExpressionType.COMBINED,
                "player value[list:s] %string% of %object%",
                "[all] player values of %object%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.pattern = pattern;
        if (pattern == 0) {
            key = (Expression<String>) exprs[0];
            player = (Expression<Object>) exprs[1];
        } else {
            player = (Expression<Object>) exprs[0];
        }
        isList = parseResult.hasTag("list");
        return true;
    }

    @Override
    protected @Nullable Object[] get(Event e) {
        if (player == null) return null;
        Object raw = player.getSingle(e);
        if (!(raw instanceof Player p)) return null;
        GamePlayer gamePlayer = playerManager.getPlayer(p);
        if (gamePlayer == null) return null;
        switch (pattern) {
            case 0:
                String k = key.getSingle(e);
                if (k == null) return null;
                Object o = gamePlayer.getValue(k);
                if (o == null) return null;
                if (o.getClass().isArray()) {
                    return (Object[]) o;
                } else {
                    return CollectionUtils.array(o);
                }
            case 1: return gamePlayer.getValues();
            default: return null;
            }
    }

    @Override
    public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> {
                if (isList) yield CollectionUtils.array(Object[].class);
                yield CollectionUtils.array(Object.class);
            }
            case ADD, REMOVE          -> CollectionUtils.array(Number.class);
            case DELETE, RESET        -> CollectionUtils.array();
            default                   -> null;
        };
    }

    @Override
    public void change(Event e, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (player == null) return;
        Object raw = player.getSingle(e);
        if (!(raw instanceof Player p)) return;
        GamePlayer gamePlayer = playerManager.getPlayer(p);
        if (gamePlayer == null) return;
        switch (mode) {
            case SET -> {
                String k = key.getSingle(e);
                if (delta == null || delta[0] == null || k == null) return;
                if (isList) {
                    gamePlayer.setValue(k, delta);
                } else {
                    gamePlayer.setValue(k, delta[0]);
                }
            }
            case ADD -> {
                String k = key.getSingle(e);
                if (delta == null || delta[0] == null || k == null) return;
                Object current = gamePlayer.getValue(k);
                if (delta[0] instanceof Number dn) {
                    double cur = (current instanceof Number cn) ? cn.doubleValue() : 0.0;
                    gamePlayer.setValue(k, cur + dn.doubleValue());
                } else {
                    Object[] arr = current instanceof Object[] a ? a : (current != null ? new Object[]{current} : new Object[0]);
                    Object[] merged = Arrays.copyOf(arr, arr.length + 1);
                    merged[arr.length] = delta[0];
                    gamePlayer.setValue(k, merged);
                }
            }
            case REMOVE -> {
                String k = key.getSingle(e);
                if (delta == null || delta[0] == null || k == null) return;
                Object current = gamePlayer.getValue(k);
                if (delta[0] instanceof Number dn) {
                    double cur = (current instanceof Number cn) ? cn.doubleValue() : 0.0;
                    gamePlayer.setValue(k, cur - dn.doubleValue());
                } else {
                    Object[] arr = current instanceof Object[] a ? a : (current != null ? new Object[]{current} : new Object[0]);
                    List<Object> list = new ArrayList<>(Arrays.asList(arr));
                    list.remove(delta[0]);
                    gamePlayer.setValue(k, list.toArray());
                }
            }
            case DELETE, RESET -> {
                if (pattern == 0) {
                    String k = key.getSingle(e);
                    if (k == null) return;
                    gamePlayer.removeValue(k);
                } else {
                    gamePlayer.removeValues();
                }
            }
        }
    }

    @Override
    public @NotNull String[] getArrayKeys(Event e) throws IllegalStateException {
        Object raw = player.getSingle(e);
        if (!(raw instanceof Player p)) return new String[0];
        GamePlayer gp = playerManager.getPlayer(p);
        return gp != null ? gp.getKeys() : new String[0];
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
