package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprSessionHost extends SimplePropertyExpression<Session, Player> {

    static {
        register(ExprSessionHost.class, Player.class,
                "host","session");
    }

    @Override
    public @Nullable Player convert(Session session) {
        return session.getHost();
    }

    @Override
    public Class<? extends Player> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET   -> CollectionUtils.array(Player.class);
            case RESET, DELETE -> CollectionUtils.array();
            default    -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Session session = getExpr().getSingle(event);
        if (session == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null) return;
                Player host = (Player) delta[0];
                if (host == null || !host.isOnline()) return;
                session.setHost(host);
            }
            case RESET, DELETE -> session.setHost(null);
        }
    }

    @Override
    protected String getPropertyName() {
        return "host";
    }

    @Override
    public Class<? extends Player> getReturnType() {
        return Player.class;
    }
}
