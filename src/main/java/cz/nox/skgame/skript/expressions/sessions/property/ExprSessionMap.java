package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - Map")
@Description({
        "Represents the game map currently assigned to a specific session.",
        "Allows you to retrieve or change which GameMap a session uses.",
        "",
        "Setting this value assigns a new GameMap to the session.",
        "Also assigning the map to session claims the Map. The map won't be returned by ",
        "Resetting this value removes the current GameMap from the session (sets it to none).",
        "If session with assigned map disbands, the map is automatically unassigned",
        "",
        "Supports: GET / SET / RESET."
})
@Examples({
        "set {_session} to session with id \"the_session_id\"",
        "set map of {_session} to gamemap with id \"arena_battle\"",
        "",
        "broadcast map of {_session}",
        "",
        "reset map of {_session}"
})
@Since("1.0.0")
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
