package cz.nox.skgame.skript.expressions.minigames;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprMiniGameValue extends SimpleExpression<Object> {
    private Expression<String> key;
    private Expression<MiniGame> miniGame;
    private int pattern;
    private int mark;

    static {
        Skript.registerExpression(ExprMiniGameValue.class, Object.class, ExpressionType.COMBINED,
                "[[mini]game] value %string% of %minigame%",
                "[all] [[mini]game] (keys|1:values) of %minigame%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.key = (Expression<String>) exprs[0];
        this.miniGame = (Expression<MiniGame>) exprs[1];
        this.pattern = pattern;
        this.mark = parseResult.mark;
        return true;
    }

    @Override
    protected Object @Nullable [] get(Event event) {
        MiniGame miniGame = this.miniGame.getSingle(event);
        if (miniGame == null) return null;
        switch (pattern) {
            case 0 -> { //Single
                Object o = miniGame.getValue(key.getSingle(event));
                return CollectionUtils.array(o);
            }
            case 1 -> { //All
                if (mark == 0) {
                    return CollectionUtils.array(miniGame.getKeys());
                } else {
                    return CollectionUtils.array(miniGame.getValues());
                }
            }
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET, DELETE -> CollectionUtils.array(Object.class);
            default          -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        MiniGame mg = this.miniGame.getSingle(event);
        String key = this.key.getSingle(event);
        if (mg == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                mg.setValue(key,delta[0]);
            }
            case DELETE, RESET -> {
                if (mg.getKeys().contains(key)) {
                    mg.setValue(key,null);
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
        return "minigame value " + this.key.toString(e,b)
                + " of minigame " + this.miniGame.toString(e,b);
    }
}
