package cz.nox.skgame.skript.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.region.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Players In Zone")
@Description({
        "Returns all players inside a named zone of the session's current map.",
        "The zone key must match a gamemap value of type 'zone' or 'arena'.",
})
@Examples({
        "set {_p::*} to players in zone \"hill_zone\" of event-session",
        "set {_p::*} to players in zone \"hill_zone\" of {_session} where [(gamemode of input) is not spectator]",
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprPlayersInZone extends SimpleExpression<Player> {

    private Expression<String> key;
    private Expression<Session> session;

    static {
        Skript.registerExpression(ExprPlayersInZone.class, Player.class, ExpressionType.COMBINED,
                "[all] players (in|within|of) zone %string% of %session%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.key = (Expression<String>) exprs[0];
        this.session = (Expression<Session>) exprs[1];
        return true;
    }

    @Override
    protected @Nullable Player[] get(Event event) {
        String k = key.getSingle(event);
        Session s = session.getSingle(event);
        if (k == null || s == null) return new Player[0];

        GameMap map = s.getGameMap();
        MiniGame mg = s.getMiniGame();
        if (map == null || mg == null) return new Player[0];

        Object value = map.getMiniGameValue(mg.getId(), k);
        if (value == null) {
            SkGame.getInstance().getLogger().warning("[SkGame/Zone] Zone key '" + k + "' not set on map '" + map.getId() + "'");
            return new Player[0];
        }
        if (!(value instanceof Region region)) {
            SkGame.getInstance().getLogger().warning("[SkGame/Zone] Zone key '" + k + "' is not a region (got " + value.getClass().getSimpleName() + ")");
            return new Player[0];
        }

        return region.getPlayers().toArray(new Player[0]);
    }

    @Override
    public boolean isSingle() { return false; }

    @Override
    public Class<? extends Player> getReturnType() { return Player.class; }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "players in zone " + key.toString(event, debug) + " of " + session.toString(event, debug);
    }
}
