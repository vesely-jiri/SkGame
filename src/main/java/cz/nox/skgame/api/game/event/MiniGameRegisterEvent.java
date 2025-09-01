package cz.nox.skgame.api.game.event;

import cz.nox.skgame.api.game.model.MiniGame;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class MiniGameRegisterEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private MiniGame miniGame;

    public MiniGameRegisterEvent(MiniGame miniGame) {
        this.miniGame = miniGame;
    }

    public MiniGame getMiniGame() {
        return this.miniGame;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
