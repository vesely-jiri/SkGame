package cz.nox.skgame.skript.expressions.gamemaps;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.GameMapManager;
import cz.nox.skgame.core.game.MiniGameManager;
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
        "# Read a single gamemap value from session (implicit map+minigame)",
        "set {_radius} to gamemap value \"spawn_radius\" of event-session",
        "",
        "# Read using explicit gamemap + minigame",
        "set {_radius} to gamemap value \"spawn_radius\" of gamemap of event-session for minigame of event-session",
        "",
        "# Read a list value (plural — defined with plurality: plural)",
        "set {_spawns::*} to gamemap values \"spawn_points\" of event-session",
        "spread session players of event-session across shuffled {_spawns::*}",
        "",
        "# Use in condition",
        "if gamemap value \"pvp_enabled\" of event-session is true:",
        "    broadcast \"PVP is on!\""
})
@Since("1.0.0")
public class ExprGameMapConfigValue extends SimpleExpression<Object> {

    private Expression<String> key;
    private Expression<GameMap> gameMap;
    private Expression<MiniGame> miniGame;
    private Expression<Object> session;

    private int pattern;
    private boolean isList;

    static {
        // COMBINED: key %string% + optional second type param make it multi-token, not pure property
        Skript.registerExpression(ExprGameMapConfigValue.class, Object.class, ExpressionType.COMBINED,
                "gamemap value[list:s] %string% of %gamemap% for %minigame%",
                "gamemap value[list:s] %string% of %object%"
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
            this.session = (Expression<Object>) exprs[1];
        }
        return true;
    }

    @Override
    public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
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
        GameMap map;
        String mgId;
        if (pattern == 0) {
            map = gameMap.getSingle(event);
            MiniGame mg = miniGame.getSingle(event);
            mgId = mg != null ? mg.getId() : null;
        } else {
            Object raw = session.getSingle(event);
            if (!(raw instanceof Session s)) return null;
            map = s.getGameMap();
            MiniGame mg = s.getMiniGame();
            mgId = mg != null ? mg.getId() : null;
        }
        if (map == null || mgId == null) return null;
        Object o = map.getMiniGameValue(mgId, k);
        if (o == null) return null;
        o = tryConvertFromString(o, mgId, k);
        if (o.getClass().isArray()) {
            return isList ? (Object[]) o : null;
        } else {
            return isList ? null : CollectionUtils.array(o);
        }
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        String k = key.getSingle(event);
        if (k == null) return;
        GameMap map;
        String mgId;
        if (pattern == 0) {
            map = gameMap.getSingle(event);
            MiniGame mg = miniGame.getSingle(event);
            mgId = mg != null ? mg.getId() : null;
        } else {
            Object raw = session.getSingle(event);
            if (!(raw instanceof Session s)) return;
            map = s.getGameMap();
            MiniGame mg = s.getMiniGame();
            mgId = mg != null ? mg.getId() : null;
        }
        if (map == null || mgId == null) return;

        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                Object val = clampWithWarn(mgId, k, delta[0]);
                map.setMiniGameValue(mgId, k, val);
                GameMapManager.getInstance().save();
            }
            case ADD -> {
                if (delta == null) return;
                for (Object o : delta) {
                    if (o != null) map.addMiniGameValue(mgId, k, clampWithWarn(mgId, k, o));
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

    private Object tryConvertFromString(Object value, String mgId, String key) {
        if (!(value instanceof String str)) return value;
        MiniGame mg = MiniGameManager.getInstance().getMiniGameById(mgId);
        if (mg == null) return value;
        CustomValue def = mg.getGameMapValueDef(key);
        if (def == null) return value;
        ClassInfo<?> ci = def.getType();
        if (ci == null || ci.getC() == String.class) return value;
        Object parsed = Classes.parse(str, ci.getC(), ParseContext.DEFAULT);
        if (parsed != null) return parsed;
        SkGame.getInstance().getLogUtil().warning(
                "Gamemap value '" + key + "' stored as String \"" + str
                + "\" but declared type is '" + ci.getCodeName() + "' — could not convert.");
        return value;
    }

    private Object clampWithWarn(String mgId, String key, Object value) {
        MiniGame mg = MiniGameManager.getInstance().getMiniGameById(mgId);
        if (mg == null) return value;
        CustomValue def = mg.getGameMapValueDef(key);
        if (def == null || !def.hasBounds()) return value;
        Object clamped = def.clamp(value);
        if (clamped != value) {
            SkGame.getInstance().getLogUtil().warning(
                    "Gamemap value '" + key + "' clamped to [" + def.getMinValue() + ", " + def.getMaxValue() + "]");
        }
        return clamped;
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
