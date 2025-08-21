package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.SessionReadOnly;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprSessionHost extends SimplePropertyExpression<SessionReadOnly, Player> {

    private static final SessionManager sessionManager = SessionManager.getInstance();

    static {
        register(ExprSessionHost.class, Player.class,
                "host","session");
    }

    @Override
    public @Nullable Player convert(SessionReadOnly session) {
        return session.getHost();
    }

    @Override
    public Class<? extends Player> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET   -> CollectionUtils.array(Player.class);
            case RESET -> CollectionUtils.array();
            default    -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        SessionReadOnly session = getExpr().getSingle(event);
        if (session == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null) return;
                Player host = (Player) delta[0];
                if (host == null || !host.isOnline()) return;
                sessionManager.setSessionHost(session.getId(), host);
            }
            case RESET -> sessionManager.setSessionHost(session.getId(),null);
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
