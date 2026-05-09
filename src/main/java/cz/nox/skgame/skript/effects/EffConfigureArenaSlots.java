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
import cz.nox.skgame.api.game.model.GameMap;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("GameMap - Configure Arena Slots")
@Description({
        "Configures the number of arena instance slots for a GameMap.",
        "Each slot is a pasted copy of the map's region at an auto-calculated location.",
        "Slots are placed along the X axis from the given base location with the given padding between them.",
        "When all configured slots are taken, a temporary slot is created automatically.",
        "",
        "The map must have a region set before configuring slots.",
        "Calling this effect again resizes the slot count (only adds, never pastes over active sessions).",
        "",
        "Default padding: 16 blocks."
})
@Examples({
        "configure arena slots of {_map} to 5 at location of player",
        "set arena slots of {_map} to 3 at {_base} with padding 32"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffConfigureArenaSlots extends Effect {

    private Expression<GameMap> gameMap;
    private Expression<Number> count;
    private Expression<Location> base;
    private @Nullable Expression<Number> padding;

    static {
        Skript.registerEffect(EffConfigureArenaSlots.class,
                "(configure|set) arena [slots] of [gamemap] %gamemap% to %number% at %location% [with [padding] %-number% [block[s]]]"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.gameMap = (Expression<GameMap>) exprs[0];
        this.count = (Expression<Number>) exprs[1];
        this.base = (Expression<Location>) exprs[2];
        this.padding = exprs[3] != null ? (Expression<Number>) exprs[3] : null;
        return true;
    }

    @Override
    protected void execute(Event event) {
        GameMap map = gameMap.getSingle(event);
        Number countVal = count.getSingle(event);
        Location baseVal = base.getSingle(event);
        if (map == null || countVal == null || baseVal == null) return;
        int slotCount = Math.max(0, countVal.intValue());
        int pad = padding != null ? Math.max(0, padding.getSingle(event).intValue()) : 16;
        map.configureArenaSlots(slotCount, baseVal, pad);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "configure arena slots of " + gameMap.toString(event, b)
                + " to " + count.toString(event, b) + " at " + base.toString(event, b);
    }
}
