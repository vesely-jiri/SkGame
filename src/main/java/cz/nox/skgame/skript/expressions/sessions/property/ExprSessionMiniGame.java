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
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - MiniGame")
@Description({"The MiniGame assigned to a session.", "", "Supports: GET / SET / RESET."})
@Examples({"set minigame of {_session} to minigame with id \"bomberman\""})
@Since("1.0.0")
public class ExprSessionMiniGame extends SimpleExpression<MiniGame> {
    private Expression<Object> expr;
    static { Skript.registerExpression(ExprSessionMiniGame.class, MiniGame.class, ExpressionType.PROPERTY,
            "[session] minigame of %session%", "%session%'s [session] minigame"); }
    @SuppressWarnings("unchecked") @Override
    public boolean init(Expression<?>[] e, int i, Kleenean k, SkriptParser.ParseResult r) { expr = (Expression<Object>) e[0]; return true; }
    @Override protected @Nullable MiniGame[] get(Event ev) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return null;
        MiniGame mg = s.getMiniGame(); return mg == null ? null : new MiniGame[]{mg}; }
    @Override public @Nullable Class<?>[] acceptChange(ChangeMode mode) {
        return switch (mode) { case SET -> CollectionUtils.array(MiniGame.class); case RESET -> CollectionUtils.array(); default -> null; }; }
    @Override public void change(Event ev, @Nullable Object[] delta, ChangeMode mode) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return;
        switch (mode) {
            case SET -> { if (delta == null || delta[0] == null) return; s.setMiniGame((MiniGame) delta[0]);
                Bukkit.getPluginManager().callEvent(new SessionSettingsChangedEvent(s, "minigame")); }
            case RESET -> { s.setMiniGame(null); Bukkit.getPluginManager().callEvent(new SessionSettingsChangedEvent(s, "minigame")); } } }
    @Override public boolean isSingle() { return true; }
    @Override public Class<MiniGame> getReturnType() { return MiniGame.class; }
    @Override public String toString(@Nullable Event ev, boolean d) { return "minigame of " + expr.toString(ev, d); }
}
