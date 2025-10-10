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
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unused")
public class ExprSessionSpectators extends SimpleExpression<Player> {
    private Expression<Session> session;

    static {
        Skript.registerExpression(ExprSessionSpectators.class, Player.class, ExpressionType.PROPERTY,
                "[all] [session] spectators of %session%"
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
        if (session != null) return session.getSpectators().toArray(new Player[0]);
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

        Player[] spectators = (delta == null) ? new Player[0] :
                Arrays.copyOf(delta, delta.length, Player[].class);

        switch (mode) {
            case SET, RESET -> {
                Player[] sessionSpectators = session.getSpectators().toArray(new Player[0]);
                session.removePlayers(sessionSpectators);
                if (mode == Changer.ChangeMode.SET)
                    session.addPlayers(spectators);
            }
            case ADD -> session.addPlayers(spectators);
            case REMOVE -> session.removePlayers(spectators);
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
        return "spectators of session " + session.toString(event,b);
    }
}
