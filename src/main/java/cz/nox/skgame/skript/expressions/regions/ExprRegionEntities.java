package cz.nox.skgame.skript.expressions.regions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.region.Region;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Region - Entities")
@Description("Returns all entities (excluding players) currently inside the given region.")
@Examples("loop entities in region {_region}:")
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprRegionEntities extends SimpleExpression<Entity> {

    private Expression<Region> region;

    static {
        Skript.registerExpression(ExprRegionEntities.class, Entity.class, ExpressionType.PROPERTY,
                "[all] entities (in|inside|of) [region] %region%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.region = (Expression<Region>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable Entity[] get(Event event) {
        Region region = this.region.getSingle(event);
        if (region == null) return new Entity[0];
        return region.getEntities(Entity.class).toArray(new Entity[0]);
    }

    @Override
    public boolean isSingle() { return false; }

    @Override
    public Class<? extends Entity> getReturnType() { return Entity.class; }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "entities in region " + region.toString(event, b);
    }
}
