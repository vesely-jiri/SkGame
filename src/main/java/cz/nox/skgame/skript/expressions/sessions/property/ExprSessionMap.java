package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprSessionMap extends SimplePropertyExpression<Session, GameMap> {

    static {
        register(ExprSessionMap.class, GameMap.class,
                "[game]map","session");
    }

    @Override
    public @Nullable GameMap convert(Session session) {
        return session.getGameMap();
    }


    @Override
    public Class<? extends GameMap> @Nullable [] acceptChange(ChangeMode mode) {
        return switch (mode) {
            case SET   -> CollectionUtils.array(GameMap.class);
            case RESET -> CollectionUtils.array();
            default    -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
        Session session = getExpr().getSingle(event);
        if (session == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                GameMap map = (GameMap) delta[0];
                session.setGameMap(map);
            }
            case RESET -> session.setGameMap(null);
        }
    }

    @Override
    protected String getPropertyName() {
        return "map";
    }

    @Override
    public Class<? extends GameMap> getReturnType() {
        return GameMap.class;
    }
}
