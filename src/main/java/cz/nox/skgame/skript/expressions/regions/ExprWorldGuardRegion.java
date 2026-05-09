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
import cz.nox.skgame.core.region.WorldGuardRegion;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Region - WorldGuard Region")
@Description({
        "Returns a Region wrapping a WorldGuard ProtectedRegion by name.",
        "Only available when WorldGuard is loaded.",
        "Containment uses the axis-aligned bounding box, approximating non-cuboid regions."
})
@Examples({
        "set region of {_map} to worldguard region \"arena\" in world \"world\"",
        "set {_r} to worldguard region \"spawn\" in world \"world_nether\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprWorldGuardRegion extends SimpleExpression<Region> {

    private Expression<String> regionId;
    private Expression<World> world;

    static {
        if (Skript.classExists("com.sk89q.worldguard.WorldGuard")) {
            Skript.registerExpression(ExprWorldGuardRegion.class, Region.class, ExpressionType.COMBINED,
                    "worldguard region %string% (in|of) [world] %world%"
            );
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.regionId = (Expression<String>) exprs[0];
        this.world = (Expression<World>) exprs[1];
        return true;
    }

    @Override
    protected @Nullable Region[] get(Event event) {
        String id = regionId.getSingle(event);
        World w = world.getSingle(event);
        if (id == null || w == null || !WorldGuardRegion.isInitialized()) return new Region[0];
        return new Region[]{new WorldGuardRegion(w, id)};
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<? extends Region> getReturnType() { return Region.class; }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "worldguard region " + regionId.toString(event, b) + " in " + world.toString(event, b);
    }
}
