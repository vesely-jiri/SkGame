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
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Spread Entities Across Locations")
@Description({
        "Teleports each entity to a location, cycling through the location list if there are fewer locations than entities.",
        "No-op if either list is empty.",
        "Shuffling must be done by the caller — this effect does not shuffle.",
})
@Examples({
        "# Teleport players to shuffled spawn points (one per player)",
        "set {_spawns::*} to gamemap values \"spawn_points\" of event-session",
        "spread shuffled (session players of event-session) across shuffled {_spawns::*}",
        "",
        "# Fewer spawns than players — locations wrap around (players share spawns)",
        "spread session players of event-session across {_two_spawns::*}",
        "",
        "# Works on any LivingEntity — mobs, animals, etc.",
        "spread {_mobs::*} across {_arena_positions::*}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSpreadAcross extends Effect {

    private Expression<LivingEntity> entities;
    private Expression<Location> locations;

    static {
        Skript.registerEffect(EffSpreadAcross.class,
                "spread %livingentities% across %locations%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.entities = (Expression<LivingEntity>) exprs[0];
        this.locations = (Expression<Location>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        LivingEntity[] entities = this.entities.getArray(event);
        Location[] locations = this.locations.getArray(event);
        if (entities.length == 0 || locations.length == 0) return;
        for (int i = 0; i < entities.length; i++) {
            entities[i].teleport(locations[i % locations.length]);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "spread " + this.entities.toString(event, b) + " across " + this.locations.toString(event, b);
    }
}
