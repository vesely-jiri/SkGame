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
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - Persistent")
@Description({"Whether this session is exempt from automatic disbanding.", "", "Supports: GET / SET."})
@Examples({"set persistent of event-session to true"})
@Since("1.0.0")
public class ExprSessionPersistent extends SimpleExpression<Boolean> {
    private Expression<Object> expr;
    static { Skript.registerExpression(ExprSessionPersistent.class, Boolean.class, ExpressionType.COMBINED,
            "[session] persist[ent] of %session%", "%session%'s [session] persist[ent]"); }
    @SuppressWarnings("unchecked") @Override
    public boolean init(Expression<?>[] e, int i, Kleenean k, SkriptParser.ParseResult r) { expr = (Expression<Object>) e[0]; return true; }
    @Override protected @Nullable Boolean[] get(Event ev) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return null; return new Boolean[]{s.isPersistent()}; }
    @Override public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return mode == Changer.ChangeMode.SET ? CollectionUtils.array(Boolean.class) : null; }
    @Override public void change(Event ev, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return;
        if (delta == null || delta[0] == null) return;
        s.setPersistent((Boolean) delta[0]); }
    @Override public boolean isSingle() { return true; }
    @Override public Class<Boolean> getReturnType() { return Boolean.class; }
    @Override public String toString(@Nullable Event ev, boolean d) { return "persistent of " + expr.toString(ev, d); }
}
