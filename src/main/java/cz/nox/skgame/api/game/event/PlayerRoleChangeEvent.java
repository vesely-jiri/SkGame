package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionRole;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fires after a session member's role has changed (PLAYER ↔ SPECTATOR).
 * Not cancellable — the state mutation already happened before this event fires.
 * Use SpectatorJoinEvent (cancellable) to intercept initial spectator join attempts.
 */
@SuppressWarnings("unused")
public class PlayerRoleChangeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final Player player;
    private final Session session;
    private final SessionRole from;
    private final SessionRole to;

    public PlayerRoleChangeEvent(Player player, Session session, SessionRole from, SessionRole to) {
        this.player = player;
        this.session = session;
        this.from = from;
        this.to = to;
    }

    public Player getPlayer() { return player; }
    public Session getSession() { return session; }
    public SessionRole getFrom() { return from; }
    public SessionRole getTo() { return to; }

    @Override public @NotNull HandlerList getHandlers() { return handlers; }
    public static HandlerList getHandlerList() { return handlers; }
}
