package cz.nox.skgame.skript.expressions.sessions.property;

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
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - Current Round")
@Description({"The current round number of a session. 0 when no game is active.", "", "Supports: GET only."})
@Examples({"broadcast current round of event-session"})
@Since("1.0.0")
public class ExprSessionCurrentRound extends SimpleExpression<Number> {
    private Expression<Object> expr;
    static { Skript.registerExpression(ExprSessionCurrentRound.class, Number.class, ExpressionType.COMBINED,
            "[session] current round of %session%", "%session%'s [session] current round",
            "current [session] round of %session%", "%session%'s current [session] round"); }
    @SuppressWarnings("unchecked") @Override
    public boolean init(Expression<?>[] e, int i, Kleenean k, SkriptParser.ParseResult r) { expr = (Expression<Object>) e[0]; return true; }
    @Override protected @Nullable Number[] get(Event ev) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return null; return new Number[]{s.getCurrentRound()}; }
    @Override public boolean isSingle() { return true; }
    @Override public Class<Number> getReturnType() { return Number.class; }
    @Override public String toString(@Nullable Event ev, boolean d) { return "current round of " + expr.toString(ev, d); }
}
