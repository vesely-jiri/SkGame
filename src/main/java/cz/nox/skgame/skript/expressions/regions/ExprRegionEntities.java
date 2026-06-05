package cz.nox.skgame.skript.expressions.regions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.entity.EntityData;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.BlockingLogHandler;
import ch.njol.skript.log.LogHandler;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.region.Region;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@Name("Region - Entities")
@Description({
        "Returns entities inside the given region.",
        "Typed form filters by entity type — supports loop aliases (loop-armor stand, loop-dropped item, etc.).",
        "Untyped form returns all entities (excluding players)."
})
@Examples({
        "loop entities in region {_region}:",
        "loop armor stands in (arena of event-session):",
        "loop dropped items in (arena of event-session):"
})
@Since("1.0.0")
@SuppressWarnings({"unused", "unchecked"})
public class ExprRegionEntities extends SimpleExpression<Entity> {

    private Expression<Region> region;
    private @Nullable Expression<EntityData> entityDataExpr;
    private int pattern;
    private Class<? extends Entity> returnType = Entity.class;

    static {
        Skript.registerExpression(ExprRegionEntities.class, Entity.class, ExpressionType.COMBINED,
                "[all] %entitydatas% (in|inside|of) [region] %skgameregion%",
                "[all] entities (in|inside|of) [region] %skgameregion%"
        );
    }

    @Override
    public boolean init(Expression<?>[] exprs, int pattern, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.pattern = pattern;
        if (pattern == 0) {
            this.entityDataExpr = (Expression<EntityData>) exprs[0];
            this.region = (Expression<Region>) exprs[1];
            if (entityDataExpr instanceof Literal<?> lit && lit.getAll().length == 1) {
                Object single = lit.getSingle();
                if (single instanceof EntityData<?> ed) returnType = ed.getType();
            }
        } else {
            this.region = (Expression<Region>) exprs[0];
        }
        return true;
    }

    @Override
    protected @Nullable Entity[] get(Event event) {
        Region r = this.region.getSingle(event);
        if (r == null) return new Entity[0];
        Collection<? extends Entity> all = r.getEntities(Entity.class);
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
        if (!(entityDataExpr instanceof Literal<?> lit))
            return false;
        try (LogHandler ignored = new BlockingLogHandler().start()) {
            EntityData<?> entityData = EntityData.parseWithoutIndefiniteArticle(s);
            if (entityData != null) {
                for (Object obj : lit.getAll()) {
                    if (!(obj instanceof EntityData<?> type)) return false;
                    if (!entityData.isSupertypeOf(type)) return false;
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isSingle() { return false; }

    @Override
    public Class<? extends Entity> getReturnType() { return returnType; }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        if (pattern == 0 && entityDataExpr != null) {
            return entityDataExpr.toString(event, b) + " in region " + region.toString(event, b);
        }
        return "entities in region " + region.toString(event, b);
    }
}
