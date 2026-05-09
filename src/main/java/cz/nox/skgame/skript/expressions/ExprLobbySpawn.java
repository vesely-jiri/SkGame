package cz.nox.skgame.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.SkGame;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Lobby Spawn")
@Description({
        "Returns the configured lobby spawn location.",
        "Can be set to change the lobby spawn (persisted to config.yml).",
        "Can be deleted/reset to clear the lobby spawn.",
})
@Examples({
        "set lobby spawn to location of player",
        "teleport player to lobby spawn",
        "delete lobby spawn"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprLobbySpawn extends SimpleExpression<Location> {

    static {
        Skript.registerExpression(ExprLobbySpawn.class, Location.class, ExpressionType.SIMPLE,
                "lobby (spawn|location)"
        );
    }

    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    protected @Nullable Location[] get(Event event) {
        Location spawn = SkGame.getInstance().getLobbySpawn();
        return spawn != null ? new Location[]{spawn} : new Location[0];
    }

    @Override
    public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
        return switch (mode) {
            case SET -> CollectionUtils.array(Location.class);
            case DELETE, RESET -> CollectionUtils.array();
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                SkGame.getInstance().setLobbySpawn((Location) delta[0]);
            }
            case DELETE, RESET -> SkGame.getInstance().setLobbySpawn(null);
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Location> getReturnType() {
        return Location.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "lobby spawn";
    }
}
