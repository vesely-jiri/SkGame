package cz.nox.skgame.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.registrations.EventValues;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.event.GameStopEvent;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Game Stop")
@Description({
        "Fires when a minigame in a session stops.",
        "",
        "You can use this event to handle scoring, cleanup, rewards, or notifications when a minigame ends.",
        "",
        "Provides the session, the minigame that stopped, and the reason for stopping.",
        "",
        "Supports: Event trigger only (GET session, GET minigame, GET reason)."
})
@Examples({
        "on game stop:",
        "    broadcast \"%id of event-minigame% in session %id of event-session% has ended\"",
        "on \"Bomberman\" game stop:",
        "    broadcast \"Bomberman minigame stopped in session %id of event-session%\""
})
@Since("1.0.0")
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
