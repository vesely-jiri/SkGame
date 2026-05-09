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
        "Returns the arena region instance assigned to a session when the game started.",
        "Only set when the session's GameMap has arena slots configured.",
        "Use `center of arena region of session` to teleport players to their instance."
})
@Examples({
        "set {_arena} to arena region of event-session",
        "teleport players of event-session to center of (arena region of event-session)"
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
