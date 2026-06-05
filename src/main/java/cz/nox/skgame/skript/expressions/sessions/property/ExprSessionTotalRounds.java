package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.event.SessionSettingsChangedEvent;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - Total Rounds")
@Description({"The total number of rounds configured for a session.", "", "Supports: GET / SET."})
@Examples({"set rounds of event-session to 3", "broadcast rounds of {_session}"})
@Since("1.0.0")
public class ExprSessionTotalRounds extends SimpleExpression<Number> {
    private Expression<Object> expr;
    static { Skript.registerExpression(ExprSessionTotalRounds.class, Number.class, ExpressionType.PROPERTY,
            "[session] rounds of %object%", "%object%'s [session] rounds"); }
    @SuppressWarnings("unchecked") @Override
    public boolean init(Expression<?>[] e, int i, Kleenean k, SkriptParser.ParseResult r) { expr = (Expression<Object>) e[0]; return true; }
    @Override protected @Nullable Number[] get(Event ev) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return null; return new Number[]{s.getTotalRounds()}; }
    @Override public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return mode == Changer.ChangeMode.SET ? CollectionUtils.array(Number.class) : null; }
    @Override public void change(Event ev, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return;
        if (delta == null || delta[0] == null) return;
        s.setTotalRounds(((Number) delta[0]).intValue());
        Bukkit.getPluginManager().callEvent(new SessionSettingsChangedEvent(s, "rounds")); }
    @Override public boolean isSingle() { return true; }
    @Override public Class<Number> getReturnType() { return Number.class; }
    @Override public String toString(@Nullable Event ev, boolean d) { return "rounds of " + expr.toString(ev, d); }
}
