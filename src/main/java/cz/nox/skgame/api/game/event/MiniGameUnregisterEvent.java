package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.MiniGame;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MiniGameUnregisterEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final MiniGame miniGame;

    public MiniGameUnregisterEvent(MiniGame miniGame) {
        this.miniGame = miniGame;
    }

    public MiniGame getMiniGame() {
        return miniGame;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}
