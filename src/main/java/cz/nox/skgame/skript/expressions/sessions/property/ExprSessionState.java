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
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - State")
@Description({"The current state of a game session (LOBBY / STARTING / STARTED / STOPPED).", "", "Supports: GET / SET / RESET."})
@Examples({"if state of {_session} is STARTED:", "    broadcast \"Running!\""})
@Since("1.0.0")
public class ExprSessionState extends SimpleExpression<SessionState> {
    private static final SessionManager manager = SessionManager.getInstance();
    private Expression<Object> expr;
    static { Skript.registerExpression(ExprSessionState.class, SessionState.class, ExpressionType.COMBINED,
            "[session] state of %session%", "%session%'s [session] state"); }
    @SuppressWarnings("unchecked") @Override
    public boolean init(Expression<?>[] e, int i, Kleenean k, SkriptParser.ParseResult r) { expr = (Expression<Object>) e[0]; return true; }
    @Override protected @Nullable SessionState[] get(Event ev) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return null;
        if (manager.getSessionById(s.getId()) == null) return null;
        return new SessionState[]{s.getState()}; }
    @Override public @Nullable Class<?>[] acceptChange(ChangeMode mode) {
        return switch (mode) { case SET -> CollectionUtils.array(SessionState.class); case RESET -> CollectionUtils.array(); default -> null; }; }
    @Override public void change(Event ev, @Nullable Object[] delta, ChangeMode mode) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof Session s)) return;
        switch (mode) {
            case SET -> { if (delta == null || delta[0] == null) return; s.setState((SessionState) delta[0]); }
            case RESET -> s.setState(SessionState.LOBBY); } }
    @Override public boolean isSingle() { return true; }
    @Override public Class<SessionState> getReturnType() { return SessionState.class; }
    @Override public String toString(@Nullable Event ev, boolean d) { return "state of " + expr.toString(ev, d); }
}
