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
public class ExprSessionPlayers extends SimpleExpression<Object> {

    private static final SessionManager sessionManager = SessionManager.getInstance();
    private Expression<SessionReadOnly> session;

    static {
        Skript.registerExpression(ExprSessionPlayers.class, Object.class, ExpressionType.PROPERTY,
                "[all] [session] players of %session%"
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
        if (session != null) {
            return session.getPlayers().toArray(new Object[0]);
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET, ADD, REMOVE, RESET -> CollectionUtils.array(Player[].class);
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
        Player[] players = (Player[]) delta[0];

        switch (mode) {
            case SET -> sessionManager.setSessionPlayers(id, players);
            case ADD -> sessionManager.addSessionPlayers(id, players);
            case REMOVE -> sessionManager.removeSessionPlayers(id, players);
            case RESET -> sessionManager.clearSessionPlayers(id);
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
        return "players of session " + session.toString();
    }
}
