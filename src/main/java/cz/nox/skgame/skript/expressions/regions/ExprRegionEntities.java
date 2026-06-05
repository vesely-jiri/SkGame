package cz.nox.skgame.skript.expressions.regions;

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
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.region.Region;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@Name("Region - Entities")
@Description({
        "Returns entities inside the given region.",
        "Use the typed form to filter by entity type (e.g. armor stands, dropped items).",
        "The untyped form returns all entities (excluding players)."
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
            EntityData<?> data = entityDataExpr.getSingle(event);
            if (data == null) return new Entity[0];
            return all.stream()
                    .filter(data::isInstance)
                    .toArray(Entity[]::new);
        }
        return all.toArray(new Entity[0]);
    }

    @Override
    public boolean isSingle() { return false; }

    @Override
    public Class<? extends Entity> getReturnType() { return Entity.class; }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        if (pattern == 0 && entityDataExpr != null) {
            return entityDataExpr.toString(event, b) + " in region " + region.toString(event, b);
        }
        return "entities in region " + region.toString(event, b);
    }
}
