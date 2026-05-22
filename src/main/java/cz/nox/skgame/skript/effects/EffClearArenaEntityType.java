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
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.region.Region;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Effect - Clear Arena Entities by Type")
@Description("Removes all non-player entities of a specific type from the arena of a session.")
@Examples("clear primed tnt from arena of event-session")
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffClearArenaEntityType extends Effect {

    private Expression<EntityType> entityType;
    private Expression<Session> session;

    static {
        Skript.registerEffect(EffClearArenaEntityType.class,
                "clear %entitytype% [entit(ies|y)] [from] arena of %session%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        this.entityType = (Expression<EntityType>) exprs[0];
        this.session = (Expression<Session>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        EntityType type = entityType.getSingle(event);
        Session s = session.getSingle(event);
        if (type == null || s == null) return;
        Region r = s.getArenaRegion();
        if (r == null || r.getWorld() == null) return;
        Class<? extends Entity> clazz = type.getEntityClass();
        if (clazz == null) return;
        r.clearEntities(clazz);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "clear " + entityType.toString(event, debug) + " from arena of " + session.toString(event, debug);
    }
}
