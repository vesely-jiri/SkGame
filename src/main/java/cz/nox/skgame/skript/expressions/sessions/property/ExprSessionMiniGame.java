package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprSessionMiniGame extends SimplePropertyExpression<Session, MiniGame> {

    static {
        register(ExprSessionMiniGame.class, MiniGame.class,
                "minigame","session");
    }

    @Override
    public @Nullable MiniGame convert(Session session) {
        return session.getMiniGame();
    }

    @Override
    public Class<? extends MiniGame> @Nullable [] acceptChange(ChangeMode mode) {
        return switch (mode) {
            case SET   -> CollectionUtils.array(MiniGame.class);
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
                MiniGame miniGame = (MiniGame) delta[0];
                session.setMiniGame(miniGame);
            }
            case ChangeMode.RESET -> session.setMiniGame(null);
        }
    }

    @Override
    protected String getPropertyName() {
        return "minigame";
    }

    @Override
    public Class<? extends MiniGame> getReturnType() {
        return MiniGame.class;
    }
}
