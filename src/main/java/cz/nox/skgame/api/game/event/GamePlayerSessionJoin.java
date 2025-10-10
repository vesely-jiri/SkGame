package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.Session;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class GamePlayerSessionJoin extends Event {
    private static final HandlerList handlers = new HandlerList();

    private Player player;
    private Session session;

    public GamePlayerSessionJoin(Player player, Session session) {
        this.player = player;
        this.session = session;
    }

    public Player getPlayer() {
        return this.player;
    }
    public Session getSession() {
        return this.session;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
