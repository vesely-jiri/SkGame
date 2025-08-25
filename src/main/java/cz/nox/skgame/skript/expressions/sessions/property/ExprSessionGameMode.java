package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.GameMode;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprSessionGameMode extends SimplePropertyExpression<Session, GameMode> {

    static {
        register(ExprSessionGameMode.class, GameMode.class,
                "gamemode","session");
    }

    @Override
    public @Nullable GameMode convert(Session session) {
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
        Session session = getExpr().getSingle(event);
        if (session == null) return;
        switch (mode) {
            case ChangeMode.SET -> {
                if (delta == null || delta[0] == null) return;
                GameMode gameMode = (GameMode) delta[0];
                session.setGameMode(gameMode);
            }
            case ChangeMode.RESET -> session.setGameMode(null);
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
