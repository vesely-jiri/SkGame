package cz.nox.skgame.skript.expressions.gamemaps;

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
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.GameMapManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("GameMap - gamemap config value")
@Description({
        "Gets or sets a gamemap config value: per-map data keyed by minigame, defined via gamemap value defs.",
        "Unlike 'value ... of gamemap of minigame', this expression resolves the minigame automatically from a session.",
        "",
        "Pattern 1: explicit gamemap + minigame.",
        "Pattern 2: gamemap and minigame resolved from session.",
        "",
        "Supports: GET / SET / ADD / REMOVE / DELETE / RESET."
})
@Examples({
        "set {_r} to gamemap value \"spawn_radius\" of gamemap of event-session for minigame of event-session",
        "set {_r} to gamemap value \"spawn_radius\" of event-session",
        "set gamemap value \"spawn_radius\" of event-session to 10"
})
@Since("1.0.0")
public class ExprGameMapConfigValue extends SimpleExpression<Object> {

    private Expression<String> key;
    private Expression<GameMap> gameMap;
    private Expression<MiniGame> miniGame;
    private Expression<Session> session;

    private int pattern;
    private boolean isList;

    static {
        Skript.registerExpression(ExprGameMapConfigValue.class, Object.class, ExpressionType.COMBINED,
                "gamemap value[list:s] %string% of %gamemap% for %minigame%",
                "gamemap value[list:s] %string% of %session%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.pattern = pattern;
        this.isList = parseResult.hasTag("list");
        this.key = (Expression<String>) exprs[0];
        if (pattern == 0) {
            this.gameMap = (Expression<GameMap>) exprs[1];
            this.miniGame = (Expression<MiniGame>) exprs[2];
        } else {
            this.session = (Expression<Session>) exprs[1];
        }
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
            default -> null;
        };
    }

    @Override
    protected @Nullable Object[] get(Event event) {
        String k = key.getSingle(event);
        if (k == null) return null;
        GameMap map = resolveMap(event);
        String mgId = resolveMgId(event);
        if (map == null || mgId == null) return null;

        Object o = map.getMiniGameValue(mgId, k);
        if (o == null) return null;
        if (o.getClass().isArray()) {
            return isList ? (Object[]) o : null;
        } else {
            return isList ? null : CollectionUtils.array(o);
        }
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        String k = key.getSingle(event);
        if (k == null) return;
        GameMap map = resolveMap(event);
        String mgId = resolveMgId(event);
        if (map == null || mgId == null) return;

        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                map.setMiniGameValue(mgId, k, delta[0]);
                GameMapManager.getInstance().save();
            }
            case ADD -> {
                if (delta == null) return;
                for (Object o : delta) {
                    if (o != null) map.addMiniGameValue(mgId, k, o);
                }
                GameMapManager.getInstance().save();
            }
            case REMOVE -> {
                if (delta == null) return;
                for (Object o : delta) {
                    if (o != null) map.removeMiniGameValue(mgId, k, o);
                }
                GameMapManager.getInstance().save();
            }
            case RESET, DELETE -> {
                map.setMiniGameValue(mgId, k, null);
                GameMapManager.getInstance().save();
            }
        }
    }

    private @Nullable GameMap resolveMap(Event event) {
        if (pattern == 0) return gameMap.getSingle(event);
        Session s = session.getSingle(event);
        return s != null ? s.getGameMap() : null;
    }

    private @Nullable String resolveMgId(Event event) {
        if (pattern == 0) {
            MiniGame mg = miniGame.getSingle(event);
            return mg != null ? mg.getId() : null;
        }
        Session s = session.getSingle(event);
        if (s == null) return null;
        MiniGame mg = s.getMiniGame();
        return mg != null ? mg.getId() : null;
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }

    @Override
    public boolean isSingle() {
        return !isList;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        String k = key.toString(event, debug);
        if (pattern == 0) {
            return "gamemap value " + k + " of " + gameMap.toString(event, debug)
                    + " for " + miniGame.toString(event, debug);
        }
        return "gamemap value " + k + " of " + session.toString(event, debug);
    }
}
