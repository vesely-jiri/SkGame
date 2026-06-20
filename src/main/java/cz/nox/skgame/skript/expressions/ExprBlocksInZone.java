package cz.nox.skgame.skript.expressions;

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
import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.region.Region;
import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Blocks In Zone")
@Description({
        "Returns all blocks inside a named zone of the session's current map.",
        "The zone key must match a gamemap value of type 'zone' or 'arena'.",
        "Warning: expensive for large zones — avoid on regions larger than ~10000 blocks.",
})
@Examples({
        "# Clear all blocks in a zone",
        "loop blocks in zone \"floor\" of event-session:",
        "    set loop-block to air",
        "",
        "# Remove TNT from a danger zone",
        "loop blocks within zone \"danger\" of {_session}:",
        "    if loop-block is tnt:",
        "        set loop-block to air"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprBlocksInZone extends SimpleExpression<Block> {

    private Expression<String> key;
    private Expression<Session> session;

    static {
        Skript.registerExpression(ExprBlocksInZone.class, Block.class, ExpressionType.COMBINED,
                "blocks (in|of|within) zone %string% of %session%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.key = (Expression<String>) exprs[0];
        this.session = (Expression<Session>) exprs[1];
        return true;
    }

    @Override
    protected @Nullable Block[] get(Event event) {
        String k = key.getSingle(event);
        Session s = session.getSingle(event);
        if (k == null || s == null) return new Block[0];

        GameMap map = s.getGameMap();
        MiniGame mg = s.getMiniGame();
        if (map == null || mg == null) return new Block[0];

        Object value = map.getMiniGameValue(mg.getId(), k);
        if (value == null) {
            SkGame.getInstance().getLogger().warning("[SkGame/Zone] Zone key '" + k + "' not set on map '" + map.getId() + "'");
            return new Block[0];
        }
        if (!(value instanceof Region region)) {
            SkGame.getInstance().getLogger().warning("[SkGame/Zone] Zone key '" + k + "' is not a region type (got " + value.getClass().getSimpleName() + ")");
            return new Block[0];
        }

        return region.getBlocks().toArray(new Block[0]);
    }

    @Override
    public boolean isSingle() { return false; }

    @Override
    public Class<? extends Block> getReturnType() { return Block.class; }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "blocks in zone " + key.toString(event, debug) + " of " + session.toString(event, debug);
    }
}
