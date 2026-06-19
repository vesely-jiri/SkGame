package cz.nox.skgame.skript.expressions.minigames;

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
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.core.game.MiniGameManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Name("MiniGame - Value")
@Description({
        "Represents a custom value stored in a specific MiniGame.",
        "You can retrieve, set, or delete values associated with a MiniGame using a key.",
        "",
        "Supports both single key access and retrieving all keys or all values.",
        "",
        "Supports: GET / SET / DELETE."
})
@Examples({
        "set {_minigame} to minigame with id \"bomberman\"",
        "set value \"max_players\" of {_minigame} to 10",
        "broadcast value \"max_players\" of {_minigame}",
        "",
        "loop values of {_minigame}:",
        "    broadcast \"Key: %loop-key% or index: %loop-index%\"",
        "    broadcast \"Value: %loop-value%\"",
        "",
        "delete value \"maxPlayers\" of {_minigame}",
        "delete values of {_minigame}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprMiniGameValue extends SimpleExpression<Object> implements KeyProviderExpression<Object> {
    private Expression<String> key;
    private Expression<MiniGame> miniGame;
    private int pattern;
    private int mark;
    @Deprecated
    private boolean isList;

    static {
        // COMBINED: key %string% param + two pattern shapes prevent pure property classification
        Skript.registerExpression(ExprMiniGameValue.class, Object.class, ExpressionType.COMBINED,
                "[[mini]game] value[list:s] %string% of %minigame%",
                "[all] [[mini]game] values of %minigame%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.pattern = pattern;
        if (pattern == 0) {
            key = (Expression<String>) exprs[0];
            miniGame = (Expression<MiniGame>) exprs[1];
        } else {
            miniGame = (Expression<MiniGame>) exprs[0];
        }
        isList = parseResult.hasTag("list");
        return true;
    }

    @Override
    protected @Nullable Object[] get(Event e) {
        MiniGame mg = canonical(miniGame.getSingle(e));
        System.err.println("[DEBUG-ExprMGValue] get: mg=" + mg + " pattern=" + pattern);
        if (mg == null) return null;
        switch (pattern) {
            case 0:
                String k = key.getSingle(e);
                if (k == null) return null;
                Object o = mg.getValue(k);
                System.err.println("[DEBUG-ExprMGValue] get result: k=" + k + " val=" + o);
                if (o == null) return null;
                if (o.getClass().isArray()) {
                    return (Object[]) o;
                } else {
                    return CollectionUtils.array(o);
                }
            case 1:  return mg.getValues();
            default: return null;
        }
    }

    @Override
    public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        System.err.println("[DEBUG-ExprMGValue] acceptChange called: mode=" + mode + " isList=" + isList);
        return switch (mode) {
            case SET           -> {
                if (isList) yield CollectionUtils.array(Object[].class);
                yield CollectionUtils.array(Object.class);
            }
            case DELETE, RESET -> CollectionUtils.array();
            default            -> null;
        };
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        MiniGame raw = miniGame.getSingle(event);
        MiniGame mg = canonical(raw);
        System.err.println("[DEBUG-ExprMGValue] change: mode=" + mode + " raw=" + raw + " mg=" + mg
                + " delta=" + (delta == null ? "null" : (delta.length == 0 ? "[]" : String.valueOf(delta[0]))));
        if (mg == null) return;
        switch (mode) {
            case SET -> {
                String k = key.getSingle(event);
                if (delta == null || delta[0] == null || k == null) {
                    System.err.println("[DEBUG-ExprMGValue] SET guard: delta=" + delta + " k=" + k);
                    return;
                }
                if (isList) {
                    mg.setValue(k, delta);
                } else {
                    mg.setValue(k,delta[0]);
                }
                System.err.println("[DEBUG-ExprMGValue] SET done: k=" + k + " stored=" + mg.getValue(k));
            }
            case DELETE, RESET -> {
                if (pattern == 0) {
                    String k = key.getSingle(event);
                    if (k == null) return;
                    mg.removeValue(k);
                } else {
                    mg.removeValues();
                }
            }
        }
    }

    private @Nullable MiniGame canonical(@Nullable MiniGame mg) {
        if (mg == null) return null;
        MiniGame real = MiniGameManager.getInstance().getMiniGameById(mg.getId());
        System.err.println("[DEBUG-ExprMGValue] canonical: id=" + mg.getId() + " real=" + real);
        return real != null ? real : mg;
    }

    @Override
    public @NotNull String[] getArrayKeys(Event e) throws IllegalStateException {
        MiniGame mg = canonical(miniGame.getSingle(e));
        assert mg != null;
        return mg.getKeys();
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
        return (!isList) && (pattern == 0);
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        if (pattern == 0) {
            return "minigame value"
                    + ((isList) ? "s " : " ")
                    + key.toString(e, b)
                    + " of minigame " + miniGame.toString(e, b);
        } else {
            return "all minigame values of minigame " + miniGame.toString(e,b);
        }
    }
}
