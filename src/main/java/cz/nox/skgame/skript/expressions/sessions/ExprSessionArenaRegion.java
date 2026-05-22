package cz.nox.skgame.skript.expressions.sessions;

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
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.region.Region;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Arena Region")
@Description({
        "Returns the arena of a session.",
        "Populated automatically when a game map is set — always non-null during a running game.",
        "For slot-based maps, reflects the specific claimed slot; for non-slotted maps, mirrors the map's region.",
        "Use this to clean up or reference the play area during game-start/stop handlers."
})
@Examples({
        "set {_arena} to arena of event-session",
        "loop players in region (arena of event-session):",
        "    teleport loop-player to location of event-session"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprSessionArenaRegion extends SimpleExpression<Region> {

    private Expression<Session> session;

    static {
        Skript.registerExpression(ExprSessionArenaRegion.class, Region.class, ExpressionType.COMBINED,
                "arena [region] of %session%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Session>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable Region[] get(Event event) {
        Session s = session.getSingle(event);
        if (s == null) return new Region[0];
        Region r = s.getArenaRegion();
        return r == null ? new Region[0] : new Region[]{r};
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<? extends Region> getReturnType() { return Region.class; }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "arena region of " + session.toString(event, b);
    }
}
