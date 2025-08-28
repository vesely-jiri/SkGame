package cz.nox.skgame.skript.expressions.sessions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@SuppressWarnings("unused")
public class ExprSessionPlayers extends SimpleExpression<Player> {

    private static final SessionManager sessionManager = SessionManager.getInstance();
    private Expression<Session> session;

    static {
        Skript.registerExpression(ExprSessionPlayers.class, Player.class, ExpressionType.PROPERTY,
                "[all] [session] players of %session%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Session>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable Player[] get(Event event) {
        Session session = this.session.getSingle(event);
        if (session != null) {
            return session.getPlayers().toArray(new Player[0]);
        }
        return null;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET, ADD, REMOVE -> CollectionUtils.array(Player[].class);
            case RESET -> CollectionUtils.array();
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Session session = this.session.getSingle(event);
        if (session == null) return;
        if (delta == null || delta[0] == null &&
                mode != Changer.ChangeMode.RESET) return;

        Player[] players = Arrays.stream(delta)
                .filter(obj -> obj instanceof Player)
                .map(obj -> (Player) obj)
                .toArray(Player[]::new);

        switch (mode) {
            case SET, RESET -> {
                Player[] sessionPlayers = session.getPlayers().toArray(new Player[0]);
                session.removePlayers(sessionPlayers);
                if (mode == Changer.ChangeMode.SET)
                    session.addPlayers(players);
            }
            case ADD -> session.addPlayers(players);
            case REMOVE -> session.removePlayers(players);
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
        Session s = this.session.getSingle(event);
        if (s == null) return null;
        return "players " + s.getPlayers() + " of  session with id " + s.getId();
    }
}
