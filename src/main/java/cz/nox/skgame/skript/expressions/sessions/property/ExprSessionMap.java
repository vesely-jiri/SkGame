package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
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
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - Map")
@Description({"The game map currently assigned to a session.", "", "Supports: GET / SET / RESET."})
@Examples({"set map of {_session} to gamemap with id \"arena_battle\"", "broadcast map of {_session}"})
@Since("1.0.0")
public class ExprSessionMap extends SimpleExpression<GameMap> {
    private Expression<Object> expr;
    static { Skript.registerExpression(ExprSessionMap.class, GameMap.class, ExpressionType.PROPERTY,
            "[session] [game]map of %object%", "%object%'s [session] [game]map"); }
    @SuppressWarnings("unchecked") @Override
    public boolean init(Expression<?>[] e, int i, Kleenean k, SkriptParser.ParseResult r) { expr = (Expression<Object>) e[0]; return true; }
    @Override protected @Nullable GameMap[] get(Event ev) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return null;
        GameMap m = s.getGameMap(); return m == null ? null : new GameMap[]{m}; }
    @Override public @Nullable Class<?>[] acceptChange(ChangeMode mode) {
        return switch (mode) { case SET -> CollectionUtils.array(GameMap.class); case RESET -> CollectionUtils.array(); default -> null; }; }
    @Override public void change(Event ev, @Nullable Object[] delta, ChangeMode mode) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return;
        switch (mode) {
            case SET -> { if (delta == null || delta[0] == null) return; s.setGameMap((GameMap) delta[0]);
                Bukkit.getPluginManager().callEvent(new SessionSettingsChangedEvent(s, "map")); }
            case RESET -> { s.setGameMap(null); Bukkit.getPluginManager().callEvent(new SessionSettingsChangedEvent(s, "map")); } } }
    @Override public boolean isSingle() { return true; }
    @Override public Class<GameMap> getReturnType() { return GameMap.class; }
    @Override public String toString(@Nullable Event ev, boolean d) { return "map of " + expr.toString(ev, d); }
}
