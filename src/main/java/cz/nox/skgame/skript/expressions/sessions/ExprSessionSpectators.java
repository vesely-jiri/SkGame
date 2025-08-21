package cz.nox.skgame.skript.expressions.sessions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.SessionReadOnly;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprSessionSpectators extends SimpleExpression<Object> {

    private static final SessionManager sessionManager = SessionManager.getInstance();
    private Expression<SessionReadOnly> session;

    static {
        Skript.registerExpression(ExprSessionSpectators.class, Object.class, ExpressionType.PROPERTY,
                "[all] [session] spectators of %session%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<SessionReadOnly>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable Object[] get(Event event) {
        SessionReadOnly session = this.session.getSingle(event);
        if (session != null) return session.getSpectators().toArray(new Object[0]);
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET, ADD, REMOVE, RESET -> CollectionUtils.array(Object.class);
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        SessionReadOnly session = this.session.getSingle(event);

        if (session == null) return;
        if (delta == null || delta[0] == null &&
                mode != Changer.ChangeMode.RESET) return;

        String id = session.getId();
        Player[] spectators = (Player[]) delta[0];

        switch (mode) {
            case SET -> sessionManager.setSessionSpectators(id,spectators);
            case ADD -> sessionManager.addSessionSpectators(id,spectators);
            case REMOVE -> sessionManager.removeSessionSpectators(id,spectators);
            case RESET -> sessionManager.clearSessionSpectators(id);
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Player> getReturnType() {
        return Player.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "spectators of session " + session.toString();
    }
}
