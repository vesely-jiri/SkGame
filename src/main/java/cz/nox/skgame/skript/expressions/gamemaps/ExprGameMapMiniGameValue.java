package cz.nox.skgame.skript.expressions.gamemaps;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.*;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("GameMap - value of MiniGame pair")
@Description({
        "Represents a custom value stored for a specific pair of GameMap and MiniGame.",
        "Use it to store or retrieve MiniGame-specific key-value data that belong to a particular GameMap.",
        "",
        "For example: a MiniGame like \"Bomberman\" may require its GameMap to remember player spawn points or item spawn locations.",
        "You can save those locations to the GameMap–MiniGame pair and retrieve them later when needed.",
        "",
        "Supports: GET / SET / RESET / DELETE.",
        "Supports: loop-index/loop-key while looping values to get the value key"
})
@Examples({
        "set {_map} to gamemap with id \"my_custom_map_id\"",
        "set {_minigame} to minigame with id \"my_custom_minigame_id\"",
        "",
        "set value \"spawnpoint\" of {_map} of {_minigame} to location of player",
        "broadcast value \"spawnpoint\" of {_map} of {_minigame}",

        "loop values of {_map} of {_minigame}:",
        "    broadcast \"key: %loop-key% or index: %loop-index%\" ",
        "    broadcast \"value: %loop-value%\"",

        "reset map value \"spawnpoint\" of {_map} of {_minigame}",
        "delete values of {_map} of {_minigame}"
        })
@Since("1.0.0")
public class ExprGameMapMiniGameValue extends SimpleExpression<Object> implements KeyProviderExpression<Object> {
    private Expression<String> key;
    private Expression<GameMap> gameMap;
    private Expression<MiniGame> miniGame;

    private int pattern;
    private int mark;
    private boolean isList;

    static {
        Skript.registerExpression(ExprGameMapMiniGameValue.class, Object.class, ExpressionType.COMBINED,
                "[pair] value[list:s] %string% of %gamemap% (of|from|and) %minigame%",
                "[all] [pair] values of %gamemap% (of|from|and) %minigame%"
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
        }
        this.isList = parseResult.hasTag("list");
        return true;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET, ADD, REMOVE -> {
                if (isList) yield CollectionUtils.array(Object[].class);
                yield CollectionUtils.array(Object.class);
            }
            case RESET, DELETE -> CollectionUtils.array();
            default            -> null;
        };
    }

    @Override
    protected @Nullable Object[] get(Event event) {
        GameMap map = gameMap.getSingle(event);
        MiniGame mg = miniGame.getSingle(event);
        if (map == null || mg == null) return null;
        String miniGameId = mg.getId();
        switch (pattern) {
            case 0:
                String k = key.getSingle(event);
                if (k == null) return null;
                Object o = map.getMiniGameValue(miniGameId, k);
                if (o == null) return null;
                if (o.getClass().isArray()) {

                    return (isList ? (Object[]) o : null);
                } else {
                    return (!isList ? CollectionUtils.array(o) : null);
                }
            case 1:  return map.getMiniGameValues(miniGameId);
            default: return null;
        }
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        GameMap map = gameMap.getSingle(event);
        MiniGame mg = miniGame.getSingle(event);
        if (map == null || mg == null) return;

        switch (mode) {
            case SET -> {
                if (pattern == 1) return;
                String k = key.getSingle(event);
                if (delta == null || delta[0] == null || k == null) return;
                map.setMiniGameValue(mg.getId(),k,delta[0]);
            }

            case ADD -> {
                if (pattern == 1) return;
                String k = key.getSingle(event);
                if (k == null || delta == null) return;

                for (Object o : delta) {
                    if (o != null) {
                        map.addMiniGameValue(mg.getId(), k, o);
                    }
                }
            }

            case REMOVE -> {
                if (pattern == 1) return;
                String k = key.getSingle(event);
                if (k == null || delta == null) return;

                for (Object o : delta) {
                    if (o != null) {
                        map.removeMiniGameValue(mg.getId(), k, o);
                    }
                }
            }

            case RESET, DELETE -> {
                if (pattern == 0) {
                    String k = key.getSingle(event);
                    if (k == null) return;
                    map.setMiniGameValue(mg.getId(), k, null);
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
        assert gm != null && mg != null;
        return gm.getMiniGameKeys(mg.getId());
    }

    @Override
    public boolean canReturnKeys() {
        return (this.pattern == 1);
    }

    @Override
    public boolean isSingle() {
        return (!isList) && (pattern == 0);
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        if (this.pattern == 0) {
            return "map value"
                    + (isList ? "s " : " ")
                    + key.toString(e, b) +
                    " of map "
                    + gameMap.toString(e, b) +
                    " of minigame "
                    + miniGame.toString(e, b);
        } else {
            return "map "
                    + " of map " + this.gameMap.toString(e,b)
                    + " of minigame " + this.miniGame.toString(e,b);
        }
    }
}
