package cz.nox.skgame.skript.expressions.regions;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import cz.nox.skgame.api.region.Region;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Region - Center")
@Description("Returns the center location of the region's bounding box.")
@Examples("teleport player to center of region {_region}")
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprRegionCenter extends SimplePropertyExpression<Region, Location> {

    static {
        register(ExprRegionCenter.class, Location.class, "center", "region");
    }

    @Override
    public @Nullable Location convert(Region region) {
        return region.getCenter();
    }

    @Override
    protected String getPropertyName() { return "center"; }

    @Override
    public Class<? extends Location> getReturnType() { return Location.class; }
}
