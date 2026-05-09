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
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Teleport Players To Lobby")
@Description({
        "Teleports all players in the session to the configured lobby spawn.",
        "No-op if no lobby spawn has been set.",
})
@Examples({
        "teleport players of {_session} to lobby"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffTeleportToLobby extends Effect {

    private Expression<Session> session;

    static {
        Skript.registerEffect(EffTeleportToLobby.class,
                "teleport players of %session% to lobby"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Session>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Session session = this.session.getSingle(event);
        if (session == null) return;
        Location lobby = SkGame.getInstance().getLobbySpawn();
        if (lobby == null) return;
        for (var player : session.getPlayers()) {
            player.teleport(lobby);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "teleport players of " + this.session.toString(event, b) + " to lobby";
    }
}
