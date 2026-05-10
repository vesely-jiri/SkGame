package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionRole;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Make Player")
@Description({
        "Changes a player's role in their session from spectator back to player.",
        "Sets the player's game mode to the value of 'defaults.gamemode' in config.yml (default: adventure).",
        "No-op if the player is not in any session.",
})
@Examples({
        "make player a player",
        "make player a player in {_session}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffMakePlayer extends Effect {

    private Expression<Player> players;
    private Expression<Session> session;

    static {
        Skript.registerEffect(EffMakePlayer.class,
                "make %players% (a|the) player [(in|of) %-session%]"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.players = (Expression<Player>) exprs[0];
        this.session = (Expression<Session>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Session explicitSession = this.session.getSingle(event);
        for (Player player : this.players.getAll(event)) {
            Session session = explicitSession != null
                    ? explicitSession
                    : SessionManager.getInstance().getSession(player);
            if (session == null) {
                SkGame.getInstance().getLogUtil().info(
                        "make player: " + player.getName() + " is not in any session");
                continue;
            }
            session.setRole(player, SessionRole.PLAYER);
            player.setGameMode(SkGame.getInstance().getDefaultGameMode());
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "make " + this.players.toString(event, b) + " a player";
    }
}
