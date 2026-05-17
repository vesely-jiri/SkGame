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
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Is In Session")
@Description({
        "Checks if a player or group of players are currently in a given session.",
        "",
        "Returns true only if all players belong to that session."
})
@Examples({
        "if player is in session {_session}:",
        "\tsend \"You’re already in that session!\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class CondIsInSession extends Condition {
    private Expression<Player> players;
    private Expression<Session> session;

    static {
        Skript.registerCondition(CondIsInSession.class,
                "%players% (is|are) in [session] %session%",
                "%players% (is not|are not|isn't|aren't) in [session] %session%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.players = (Expression<Player>) exprs[0];
        this.session = (Expression<Session>) exprs[1];
        setNegated(i == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        Session session = this.session.getSingle(event);
        if (session == null) return isNegated();
        return players.check(event, player -> session.getRole(player) != null, isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "Players " + players.toString(event,b) + " (is|are) " + (b ? "" : "not ")
                + "in session " + session.toString(event,b);
    }
}
