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
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Is Player")
@Description({
        "Checks whether a player or players have the PLAYER role in a session (not spectator).",
        "If no session is provided, uses each player's current session.",
        "Returns true only if ALL players satisfy the condition."
})
@Examples({
        "if player is a player in {_session}:",
        "if player is not a player:"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class CondIsPlayer extends Condition {

    private Expression<Player> players;
    private Expression<Session> session;

    static {
        Skript.registerCondition(CondIsPlayer.class,
                "%players% (is|are) [a] player [(in|of) %-session%]",
                "%players% (is not|are not|isn't|aren't) [a] player [(in|of) %-session%]"
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
        return players.check(event, player -> {
            Session s = resolveSession(player, event);
            return s != null && s.getRole(player) == SessionRole.PLAYER;
        }, isNegated());
    }

    private Session resolveSession(Player player, Event event) {
        Session explicit = this.session.getSingle(event);
        return explicit != null ? explicit : SessionManager.getInstance().getSession(player);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return this.players.toString(event, b) + (isNegated() ? " is not" : " is") + " a player";
    }
}
