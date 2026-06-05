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
import cz.nox.skgame.api.game.model.SessionVisibility;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - Visibility")
@Description({"The visibility of a session: PUBLIC or PRIVATE.", "", "Supports: GET / SET."})
@Examples({"set session visibility of event-session to private"})
@Since("1.0.0")
public class ExprSessionVisibility extends SimpleExpression<SessionVisibility> {
    private Expression<Object> expr;
    static { Skript.registerExpression(ExprSessionVisibility.class, SessionVisibility.class, ExpressionType.COMBINED,
            "[session] visibility of %session%", "%session%'s [session] visibility"); }
    @SuppressWarnings("unchecked") @Override
    public boolean init(Expression<?>[] e, int i, Kleenean k, SkriptParser.ParseResult r) { expr = (Expression<Object>) e[0]; return true; }
    @Override protected @Nullable SessionVisibility[] get(Event ev) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return null;
        SessionVisibility v = s.getVisibility(); return v == null ? null : new SessionVisibility[]{v}; }
    @Override public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return mode == Changer.ChangeMode.SET ? CollectionUtils.array(SessionVisibility.class) : null; }
    @Override public void change(Event ev, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return;
        if (delta == null || delta[0] == null) return;
        s.setVisibility((SessionVisibility) delta[0]);
        Bukkit.getPluginManager().callEvent(new SessionSettingsChangedEvent(s, "visibility")); }
    @Override public boolean isSingle() { return true; }
    @Override public Class<SessionVisibility> getReturnType() { return SessionVisibility.class; }
    @Override public String toString(@Nullable Event ev, boolean d) { return "visibility of " + expr.toString(ev, d); }
}
