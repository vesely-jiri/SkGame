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
import cz.nox.skgame.core.region.CuboidRegion;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Region - New Cuboid Region")
@Description("Creates a new cuboid region between two corner locations.")
@Examples("set region of {_map} to cuboid region from {_pos1} to {_pos2}")
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprCuboidRegion extends SimpleExpression<Region> {

    private Expression<Location> corner1;
    private Expression<Location> corner2;

    static {
        // COMBINED: constructor pattern (two location params), not property-of-type
        Skript.registerExpression(ExprCuboidRegion.class, Region.class, ExpressionType.COMBINED,
                "[a] [new] cuboid [region] (from|between) %location% (to|and) %location%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.corner1 = (Expression<Location>) exprs[0];
        this.corner2 = (Expression<Location>) exprs[1];
        return true;
    }

    @Override
    protected @Nullable Region[] get(Event event) {
        Location c1 = corner1.getSingle(event);
        Location c2 = corner2.getSingle(event);
        if (c1 == null || c2 == null || c1.getWorld() == null || c2.getWorld() == null) return new Region[0];
        if (!c1.getWorld().equals(c2.getWorld())) return new Region[0];
        return new Region[]{new CuboidRegion(c1, c2)};
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<? extends Region> getReturnType() { return Region.class; }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "cuboid region from " + corner1.toString(event, b) + " to " + corner2.toString(event, b);
    }
}
