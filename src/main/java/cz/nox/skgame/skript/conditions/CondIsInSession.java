package cz.nox.skgame.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@SuppressWarnings("unused")
public class CondIsInSession extends Condition {
    private static final SessionManager sessionManager = SessionManager.getInstance();
    private Expression<Player> player;
    private Expression<Session> session;

    static {
        Skript.registerCondition(CondIsInSession.class, ConditionType.PROPERTY,
                "%player% (of|in) [session] %session%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.player = (Expression<Player>) exprs[0];
        this.session = (Expression<Session>) exprs[1];
        return true;
    }

    @Override
    public boolean check(Event event) {
        Player player = this.player.getSingle(event);
        var single = this.session.getSingle(event);
        if (single == null || player == null) return false;
        return single.getPlayers().contains(player);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "Player " + player + " is " + (b ? "" : "not ") + "in session " + session;
    }
}
