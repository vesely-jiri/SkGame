package cz.nox.skgame.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.BlockingLogHandler;
import ch.njol.skript.log.LogHandler;
import ch.njol.util.Kleenean;
import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.region.Region;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@Name("Entities In Zone")
@Description({
        "Returns entities inside a named zone of the session's current map.",
        "Typed form filters by entity type (e.g. 'zombies in zone ...', 'dropped items in zone ...').",
        "Untyped form returns all entities.",
        "The zone key must match a gamemap value of type 'zone' or 'arena'.",
})
@Examples({
        "set {_e::*} to entities in zone \"spawn_zone\" of event-session",
        "set {_z::*} to zombies in zone \"spawn_zone\" of {_session}",
        "loop dropped items in zone \"arena\" of event-session:",
        "    remove loop-entity",
})
@Since("1.0.0")
@SuppressWarnings({"unused", "unchecked"})
public class ExprEntitiesInZone extends SimpleExpression<Entity> {

    private Expression<String> key;
    private Expression<Session> session;
    private @Nullable Expression<EntityData> entityDataExpr;
    private int pattern;
    private Class<? extends Entity> returnType = Entity.class;

    static {
        Skript.registerExpression(ExprEntitiesInZone.class, Entity.class, ExpressionType.COMBINED,
                "[all] %entitydatas% (in|within|of) zone %string% of %session%",
                "[all] entities (in|within|of) zone %string% of %session%"
        );
    }

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.pattern = pattern;
        if (pattern == 0) {
            this.entityDataExpr = (Expression<EntityData>) exprs[0];
            this.key     = (Expression<String>)  exprs[1];
            this.session = (Expression<Session>) exprs[2];
            Class<?> srcType = entityDataExpr.getSource().getReturnType();
            if (srcType != null && Entity.class.isAssignableFrom(srcType) && !Entity.class.equals(srcType)) {
                returnType = (Class<? extends Entity>) srcType;
            }
        } else {
            this.key     = (Expression<String>)  exprs[0];
            this.session = (Expression<Session>) exprs[1];
        }
        return true;
    }

    @Override
    protected @Nullable Entity[] get(Event event) {
        String k = key.getSingle(event);
        Session s = session.getSingle(event);
        if (k == null || s == null) return new Entity[0];

        GameMap map = s.getGameMap();
        MiniGame mg = s.getMiniGame();
        if (map == null || mg == null) return new Entity[0];

        Object value = map.getMiniGameValue(mg.getId(), k);
        if (value == null) {
            SkGame.getInstance().getLogger().warning("[SkGame/Zone] Zone key '" + k + "' not set on map '" + map.getId() + "'");
            return new Entity[0];
        }
        if (!(value instanceof Region region)) {
            SkGame.getInstance().getLogger().warning("[SkGame/Zone] Zone key '" + k + "' is not a region (got " + value.getClass().getSimpleName() + ")");
            return new Entity[0];
        }

        Collection<? extends Entity> all = region.getEntities(Entity.class);
        if (pattern == 0 && entityDataExpr != null) {
            EntityData<?>[] data = entityDataExpr.getArray(event);
            if (data.length == 0) return new Entity[0];
            return all.stream()
                    .filter(e -> {
                        for (EntityData<?> d : data) if (d.isInstance(e)) return true;
                        return false;
                    })
                    .toArray(Entity[]::new);
        }
        return all.toArray(new Entity[0]);
    }

    @Override
    public boolean isLoopOf(String s) {
        try (LogHandler ignored = new BlockingLogHandler().start()) {
            EntityData<?> loopData = EntityData.parseWithoutIndefiniteArticle(s);
            if (loopData != null) {
                return loopData.getType().isAssignableFrom(returnType);
            }
        }
        return false;
    }

    @Override
    public boolean isSingle() { return false; }

    @Override
    public Class<? extends Entity> getReturnType() { return returnType; }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        if (pattern == 0 && entityDataExpr != null) {
            return entityDataExpr.toString(event, debug) + " in zone " + key.toString(event, debug) + " of " + session.toString(event, debug);
        }
        return "entities in zone " + key.toString(event, debug) + " of " + session.toString(event, debug);
    }
}
