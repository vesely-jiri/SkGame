package cz.nox.skgame.skript.expressions.gamemaps;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprGameMapValue extends SimpleExpression<Object> {
    private Expression<String> key;
    private Expression<GameMap> gameMap;
    private Expression<MiniGame> miniGame;
    private int pattern;
    private int mark;

    static {
        Skript.registerExpression(ExprGameMapValue.class, Object.class, ExpressionType.COMBINED,
                "[[game]map] value %string% of %gamemap% (of|from|and) %minigame%",
                "[all] (keys|1:values) of %gamemap% (of|from|and) %minigame%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.key = (Expression<String>) exprs[0];
        this.gameMap = (Expression<GameMap>) exprs[1];
        this.miniGame = (Expression<MiniGame>) exprs[2];
        this.pattern = pattern;
        this.mark = parseResult.mark;
        return true;
    }


    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET           -> CollectionUtils.array(Object.class);
            case RESET, DELETE -> CollectionUtils.array();
            default            -> null;
        };
    }

    @Override
    protected @Nullable Object[] get(Event event) {
        String k = this.key.getSingle(event);
        GameMap map = this.gameMap.getSingle(event);
        MiniGame gm = this.miniGame.getSingle(event);
        if (k == null || map == null || gm == null) return null;
        Object o = map.getMiniGameValue(gm.getId(), this.key.getSingle(event));
        if (o instanceof Object[]) {
            return (Object[]) o;
        } else {
            return CollectionUtils.array(o);
        }
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        GameMap map = this.gameMap.getSingle(event);
        MiniGame gm = this.miniGame.getSingle(event);
        String k = this.key.getSingle(event);
        if (map == null || gm == null || k == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                Object o = delta[0];
                map.setMiniGameValue(gm.getId(),k,o);
            }
            case RESET, DELETE -> map.setMiniGameValue(gm.getId(),k,null);
        }
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public boolean isSingle() {
        return this.pattern == 0;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "map value " + this.key.toString(event,b)
                + " of map " + this.gameMap.toString(event,b)
                + " of minigame " + this.miniGame.toString(event,b);
    }
}
