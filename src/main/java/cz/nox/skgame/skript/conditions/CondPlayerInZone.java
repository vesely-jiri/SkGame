package cz.nox.skgame.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.region.Region;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Player Is In Zone")
@Description({
        "Checks whether a player is inside a named zone of the session's current map.",
        "The zone key must match a gamemap value of type 'zone' or 'arena'.",
        "Returns false (with a warning) if the key is missing or the value is not a region.",
})
@Examples({
        "if event-player is in zone \"flag_zone_red\" of event-session:",
        "    broadcast \"Red flag captured!\"",
        "if player is not in zone \"safe_area\" of {_session}:",
        "    damage player by 5"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class CondPlayerInZone extends Condition {

    private Expression<Player> player;
    private Expression<String> key;
    private Expression<Session> session;

    static {
        Skript.registerCondition(CondPlayerInZone.class,
                "%player% is in zone %string% of %session%",
                "%player% is(n't| not) in zone %string% of %session%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.player = (Expression<Player>) exprs[0];
        this.key = (Expression<String>) exprs[1];
        this.session = (Expression<Session>) exprs[2];
        setNegated(i == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        Player p = player.getSingle(event);
        String k = key.getSingle(event);
        Session s = session.getSingle(event);
        if (p == null || k == null || s == null) return isNegated();

        GameMap map = s.getGameMap();
        MiniGame mg = s.getMiniGame();
        if (map == null || mg == null) return isNegated();

        Object value = map.getMiniGameValue(mg.getId(), k);
        if (value == null) {
            SkGame.getInstance().getLogger().warning("[SkGame/Zone] Zone key '" + k + "' not set on map '" + map.getId() + "'");
            return isNegated();
        }
        if (!(value instanceof Region region)) {
            SkGame.getInstance().getLogger().warning("[SkGame/Zone] Zone key '" + k + "' is not a region type (got " + value.getClass().getSimpleName() + ")");
            return isNegated();
        }

        boolean inside = region.contains(p.getLocation());
        return isNegated() != inside;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return player.toString(event, debug) + (isNegated() ? " is not" : " is")
                + " in zone " + key.toString(event, debug)
                + " of " + session.toString(event, debug);
    }
}
