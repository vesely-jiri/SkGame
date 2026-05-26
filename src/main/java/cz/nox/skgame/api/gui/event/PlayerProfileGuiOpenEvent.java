package cz.nox.skgame.api.gui.event;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerProfileGuiOpenEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;
    private final Player viewer;
    private final OfflinePlayer subject;

    public PlayerProfileGuiOpenEvent(Player viewer, OfflinePlayer subject) {
        this.viewer = viewer;
        this.subject = subject;
    }

    public Player getViewer() { return viewer; }
    public OfflinePlayer getSubject() { return subject; }

    @Override public boolean isCancelled() { return cancelled; }
    @Override public void setCancelled(boolean cancel) { this.cancelled = cancel; }
    @Override public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
