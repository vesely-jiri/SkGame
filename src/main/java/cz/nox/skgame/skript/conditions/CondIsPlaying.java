package cz.nox.skgame.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.GameMode;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class CondIsPlaying extends Condition {
    private static final SessionManager sessionManager = SessionManager.getInstance();
    private Expression<Player> players;
    private Expression<GameMode> gameMode;

    static {
        Skript.registerCondition(CondIsPlaying.class,
                "%players% (is|are) playing %gamemode%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.players = (Expression<Player>) exprs[0];
        this.gameMode = (Expression<GameMode>) exprs[1];
        return true;
    }

    @Override
    public boolean check(Event event) {
        GameMode gameMode = this.gameMode.getSingle(event);
        for (Player player : this.players.getAll(event)) {
            Session session = sessionManager.getSession(player);
            if (session == null) return false;
            if (session.getGameMode() != gameMode) return false;
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "Player[s] " + players.getSingle(event)
                + "are" + (b ? "" : "not ") + "playing "
                + gameMode.getSingle(event);
    }

    // TODO - is isSingle required here?
}
