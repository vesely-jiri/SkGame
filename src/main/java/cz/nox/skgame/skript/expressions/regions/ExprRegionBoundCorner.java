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
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Region - Min/Max Corner")
@Description("Returns the minimum or maximum corner location of the region.")
@Examples({
        "set {_min} to minimum corner of region {_region}",
        "set {_max} to maximum corner of region {_region}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprRegionBoundCorner extends SimpleExpression<Location> {

    // 0 = min, 1 = max
    private int matchIndex;
    private Expression<Region> region;

    static {
        Skript.registerExpression(ExprRegionBoundCorner.class, Location.class, ExpressionType.PROPERTY,
                "min[imum] [corner] of [region] %region%",
                "max[imum] [corner] of [region] %region%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.matchIndex = i;
        this.region = (Expression<Region>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable Location[] get(Event event) {
        Region region = this.region.getSingle(event);
        if (region == null) return new Location[0];
        Location loc = matchIndex == 0 ? region.getMin() : region.getMax();
        return new Location[]{loc};
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<? extends Location> getReturnType() { return Location.class; }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return (matchIndex == 0 ? "minimum" : "maximum") + " corner of region " + region.toString(event, b);
    }
}
