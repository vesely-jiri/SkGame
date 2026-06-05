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
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - host")
@Description({"Represents host of a game session.", "", "Supports: GET / SET / RESET."})
@Examples({"set host of {_session} to player", "broadcast host of {_session}"})
@Since("1.0.0")
public class ExprSessionHost extends SimpleExpression<Player> {
    private Expression<Object> expr;
    static { Skript.registerExpression(ExprSessionHost.class, Player.class, ExpressionType.PROPERTY,
            "[session] host of %object%", "%object%'s [session] host"); }
    @SuppressWarnings("unchecked") @Override
    public boolean init(Expression<?>[] e, int i, Kleenean k, SkriptParser.ParseResult r) { expr = (Expression<Object>) e[0]; return true; }
    @Override protected @Nullable Player[] get(Event ev) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return null;
        Player h = s.getHost(); return h == null ? null : new Player[]{h}; }
    @Override public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> CollectionUtils.array(Player.class);
            case RESET, DELETE -> CollectionUtils.array();
            default -> null; }; }
    @Override public void change(Event ev, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return;
        switch (mode) {
            case SET -> { if (delta == null) return; Player h = (Player) delta[0];
                if (h == null || !h.isOnline()) return; s.setHost(h); }
            case RESET, DELETE -> s.setHost(null); } }
    @Override public boolean isSingle() { return true; }
    @Override public Class<Player> getReturnType() { return Player.class; }
    @Override public String toString(@Nullable Event ev, boolean d) { return "host of " + expr.toString(ev, d); }
}
