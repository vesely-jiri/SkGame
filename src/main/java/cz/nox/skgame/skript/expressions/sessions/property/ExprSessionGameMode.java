package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.GameMode;
import cz.nox.skgame.api.game.model.SessionReadOnly;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprSessionGameMode extends SimplePropertyExpression<SessionReadOnly, GameMode> {

    private static final SessionManager sessionManager = SessionManager.getInstance();

    static {
        register(ExprSessionGameMode.class, GameMode.class,
                "gamemode","session");
    }

    @Override
    public @Nullable GameMode convert(SessionReadOnly session) {
        return session.getGameMode();
    }

    @Override
    public Class<? extends GameMode> @Nullable [] acceptChange(ChangeMode mode) {
        return switch (mode) {
            case SET   -> CollectionUtils.array(GameMode.class);
            case RESET -> CollectionUtils.array();
            default    -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
        SessionReadOnly session = getExpr().getSingle(event);
        if (session == null) return;
        switch (mode) {
            case ChangeMode.SET -> {
                if (delta == null || delta[0] == null) return;
                GameMode gameMode = (GameMode) delta[0];
                sessionManager.setSessionGameMode(session.getId(),gameMode);
            }
            case ChangeMode.RESET -> sessionManager.setSessionGameMode(session.getId(),null);
        }
    }

    @Override
    protected String getPropertyName() {
        return "gamemode";
    }

    @Override
    public Class<? extends GameMode> getReturnType() {
        return GameMode.class;
    }
}
