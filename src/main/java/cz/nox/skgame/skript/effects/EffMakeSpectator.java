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
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Make Spectator")
@Description({
        "Changes a player's role in their session to spectator.",
        "Sets the player's game mode to the value of 'spectators.gamemode' in config.yml (default: spectator).",
        "Teleports the player to the spectator spawn: map value 'spectator_spawn' → region center → lobby.",
        "No-op if the player is not in any session.",
})
@Examples({
        "make player a spectator",
        "make player a spectator in {_session}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffMakeSpectator extends Effect {

    private Expression<Player> players;
    private Expression<Session> session;

    static {
        Skript.registerEffect(EffMakeSpectator.class,
                "make %players% (a|the) spectator [(in|of) %-session%]"
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
        GameMode spectatorMode = resolveSpectatorGameMode();
        Session explicitSession = this.session.getSingle(event);
        for (Player player : this.players.getAll(event)) {
            Session session = explicitSession != null
                    ? explicitSession
                    : SessionManager.getInstance().getSession(player);
            if (session == null) {
                SkGame.getInstance().getLogUtil().info(
                        "make spectator: " + player.getName() + " is not in any session");
                continue;
            }
            session.setRole(player, SessionRole.SPECTATOR);
            player.setGameMode(spectatorMode);
            Location spawn = SessionManager.resolveSpectatorSpawn(session);
            if (spawn != null) player.teleport(spawn);
        }
    }

    private GameMode resolveSpectatorGameMode() {
        String raw = SkGame.getInstance().getConfig().getString("spectators.gamemode", "spectator");
        try {
            return GameMode.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GameMode.SPECTATOR;
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "make " + this.players.toString(event, b) + " a spectator";
    }
}
