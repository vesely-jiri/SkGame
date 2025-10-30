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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprGameMapMiniGameValue extends SimpleExpression<Object> {
    private Expression<String> key;
    private Expression<GameMap> gameMap;
    private Expression<MiniGame> miniGame;
    private int pattern;
    private int mark;

    static {
        Skript.registerExpression(ExprGameMapMiniGameValue.class, Object.class, ExpressionType.COMBINED,
                "[[game]map] value %string% of %gamemap% (of|from|and) %minigame%",
                "[all] (keys|1:values) of %gamemap% (of|from|and) %minigame%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.pattern = pattern;
        if (pattern == 0) {
            this.key = (Expression<String>) exprs[0];
            this.gameMap = (Expression<GameMap>) exprs[1];
            this.miniGame = (Expression<MiniGame>) exprs[2];
        } else {
            this.gameMap = (Expression<GameMap>) exprs[0];
            this.miniGame = (Expression<MiniGame>) exprs[1];
            this.mark = parseResult.mark;
        }
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
        GameMap map = this.gameMap.getSingle(event);
        MiniGame mg = this.miniGame.getSingle(event);
        if (map == null || mg == null) return null;
        String miniGameId = mg.getId();
        Object o;
        if (this.pattern == 0) {
            String k = this.key.getSingle(event);
            if (k == null) return null;
            o = map.getMiniGameValue(miniGameId, this.key.getSingle(event));
        } else {
            if (this.mark == 0) {
                o = map.getMiniGameKeys(miniGameId);
            } else {
                o = map.getMiniGameValues(miniGameId);
            }
        }
        if (o instanceof Object[]) {
            return (Object[]) o;
        } else {
            return CollectionUtils.array(o);
        }
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {

        GameMap map = this.gameMap.getSingle(event);
        MiniGame mg = this.miniGame.getSingle(event);
        if (map == null || mg == null) return;

        if (this.pattern == 0) {
            String k = this.key.getSingle(event);
            if (k == null) return;
        }

        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                if (pattern == 1) return;
                Object o = delta[0];
                map.setMiniGameValue(mg.getId(),this.key.getSingle(event),o);
            }
            case RESET, DELETE -> {
                if (pattern == 0) {
                    map.setMiniGameValue(mg.getId(), this.key.getSingle(event), null);
                } else {
                    map.setMiniGameValues(mg.getId(),null);
                }
            }
        }
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public boolean isLoopOf(String input) {
        if (input.equals("key") || input.equals("index")) {
            return true;
        }
        return super.isLoopOf(input);
    }

    @Override
    public boolean isIndexLoop(String input) {
        if (input.equals("key") || input.equals("index")) {
            return true;
        }
        return KeyProviderExpression.super.isIndexLoop(input);
    }

    @Override
    public @NotNull String @NotNull [] getArrayKeys(Event e) throws IllegalStateException {
        GameMap gm = this.gameMap.getSingle(e);
        MiniGame mg = this.miniGame.getSingle(e);
        if (mg == null || gm == null) return new String[0];
        return gm.getMiniGameKeys(mg.getId());
    }

    @Override
    public boolean canReturnKeys() {
        return (this.pattern == 1) && ((this.mark == 1));
    }

    @Override
    public boolean isSingle() {
        return (this.pattern == 0);
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        if (this.pattern == 0) {
            return "map value " + this.key.toString(e, b) +
                    " of map " + this.gameMap.toString(e, b) +
                    " of minigame " + this.miniGame.toString(e, b);
        } else {
            return "map " +
                    ((this.mark == 1) ? "values" : "keys") +
                    " of map " + this.gameMap.toString(e,b) +
                    " of minigame " + this.miniGame.toString(e,b);
        }
    }
}
