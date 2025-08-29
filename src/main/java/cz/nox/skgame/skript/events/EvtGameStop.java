package cz.nox.skgame.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.event.GameStopEvent;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Called when minigame of session stops
 */
@SuppressWarnings("unused")
public class EvtGameStop extends SkriptEvent {
    private Literal<String> miniGameId;

    static {
        Skript.registerEvent("GameStop", EvtGameStop.class, GameStopEvent.class,
                "[%string%] game stop",
                "game [%string%] stop"
        );
        EventValues.registerEventValue(GameStopEvent.class, Session.class, GameStopEvent::getSession, EventValues.TIME_NOW);
        EventValues.registerEventValue(GameStopEvent.class, MiniGame.class, GameStopEvent::getMiniGame, EventValues.TIME_NOW);
        EventValues.registerEventValue(GameStopEvent.class, String.class, GameStopEvent::getReason, EventValues.TIME_NOW);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int i, SkriptParser.ParseResult parseResult) {
        if (args.length > 0 && args[0] != null) {
            this.miniGameId = (Literal<String>) args[0];
        }
        return true;
    }

    @Override
    public boolean check(Event e) {
        GameStopEvent event = (GameStopEvent) e;
        if (miniGameId == null) {
            return true;
        }
        String expected = miniGameId.getSingle(e);
        if (expected == null) {
            return true;
        }
        return expected.equalsIgnoreCase(event.getMiniGame().getId());
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        String id = (miniGameId == null || event == null) ? "any" : miniGameId.toString(event, b);
        return "on " + id + " game stop";
    }
}
