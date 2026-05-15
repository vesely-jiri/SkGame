package cz.nox.skgame.api.gui.event;

import cz.nox.skgame.api.game.model.Session;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class MinigamesGuiOpenEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Session session;
    private boolean cancelled;

    public MinigamesGuiOpenEvent(Player player, Session session) {
        this.player = player;
        this.session = session;
    }

    public Player getPlayer() { return player; }
    public Session getSession() { return session; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
