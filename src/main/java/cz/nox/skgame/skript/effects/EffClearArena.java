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
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Effect - Clear Arena")
@Description("Removes all non-player entities from the arena of a session.")
@Examples("clear arena of event-session")
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffClearArena extends Effect {

    private Expression<Session> session;

    static {
        Skript.registerEffect(EffClearArena.class,
                "clear [all] entit(ies|y) [from] arena of %session%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Session>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Session s = session.getSingle(event);
        if (s == null) return;
        Region r = s.getArenaRegion();
        if (r == null || r.getWorld() == null) return;
        r.clearEntities(Entity.class);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "clear arena of " + session.toString(event, debug);
    }
}
