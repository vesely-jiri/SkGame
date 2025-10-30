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
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("GamePlayer - Is Playing")
@Description({
        "Checks whether a player or players are currently playing a specific MiniGame.",
        "",
        "Returns true only if all players are in an active session of that MiniGame."
})
@Examples({
        "if player is playing {_minigame}:",
        "\tbroadcast \"%player% is playing %{_minigame}%!\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class CondIsPlaying extends Condition {
    private static final SessionManager sessionManager = SessionManager.getInstance();
    private Expression<Player> players;
    private Expression<MiniGame> miniGame;

    static {
        Skript.registerCondition(CondIsPlaying.class,
                "%players% (is|are) playing %minigame%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.players = (Expression<Player>) exprs[0];
        this.miniGame = (Expression<MiniGame>) exprs[1];
        return true;
    }

    @Override
    public boolean check(Event event) {
        MiniGame miniGame = this.miniGame.getSingle(event);
        for (Player player : this.players.getAll(event)) {
            Session session = sessionManager.getSession(player);
            if (session == null) return false;
            if (session.getMiniGame() != miniGame) return false;
            if (session.getState() != SessionState.STARTED) return false;
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return players.getSingle(event)
                + "(is|are)" + (b ? " " : " not ") + "playing "
                + miniGame.getSingle(event);
    }
}
