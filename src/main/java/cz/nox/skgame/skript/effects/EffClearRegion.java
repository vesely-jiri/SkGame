package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.region.Region;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Region - Clear Entities")
@Description({
        "Removes all non-player entities from the given region.",
        "Players are never removed regardless of this effect."
})
@Examples("clear entities in region {_region}")
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffClearRegion extends Effect {

    private Expression<Region> region;

    static {
        Skript.registerEffect(EffClearRegion.class,
                "clear [all] entit(ies|y) (in|from) [region] %skarena%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.region = (Expression<Region>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Region region = this.region.getSingle(event);
        if (region == null) return;
        region.clearEntities(Entity.class);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "clear entities in region " + this.region.toString(event, b);
    }
}
