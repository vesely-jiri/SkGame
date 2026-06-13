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
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Region - Blocks")
@Description({
        "Returns all blocks inside the given region.",
        "Warning: expensive for large regions — avoid on regions larger than ~10000 blocks."
})
@Examples("loop blocks in region {_region}:")
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprRegionBlocks extends SimpleExpression<Block> {

    private Expression<Region> region;

    static {
        Skript.registerExpression(ExprRegionBlocks.class, Block.class, ExpressionType.PROPERTY,
                "[all] blocks (in|inside|of) [region] %skarena%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.region = (Expression<Region>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable Block[] get(Event event) {
        Region region = this.region.getSingle(event);
        if (region == null) return new Block[0];
        return region.getBlocks().toArray(new Block[0]);
    }

    @Override
    public boolean isSingle() { return false; }

    @Override
    public Class<? extends Block> getReturnType() { return Block.class; }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "blocks in region " + region.toString(event, b);
    }
}
