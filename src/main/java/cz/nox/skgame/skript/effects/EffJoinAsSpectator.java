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
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Join As Spectator")
@Description({
        "Attempts to add a non-member player to a session as a spectator.",
        "Fires SpectatorJoinEvent (cancellable). If cancelled, the player is not added.",
        "If the player is already a session member in any role, this is a no-op — use",
        "'make %player% a spectator' to change the role of an existing member.",
})
@Examples({
        "player join {_session} as spectator",
        "if player is not a spectator in {_session}:",
        "    send skgame message \"spectator.join-denied\" to player"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffJoinAsSpectator extends Effect {

    private Expression<Player> player;
    private Expression<Session> session;

    static {
        Skript.registerEffect(EffJoinAsSpectator.class,
                "%player% join[s] %session% as [a] spectator"
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
    protected void execute(Event event) {
        Player player = this.player.getSingle(event);
        Session session = this.session.getSingle(event);
        if (player == null || session == null) return;
        SessionManager.getInstance().joinAsSpectator(player, session);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return this.player.toString(event, b) + " join " + this.session.toString(event, b) + " as spectator";
    }
}
