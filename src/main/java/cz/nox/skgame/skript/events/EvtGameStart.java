package cz.nox.skgame.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Game Start")
@Description({
        "Fires when a minigame in a session starts.",
        "",
        "You can use this event to prepare the map, initialize player states, or trigger announcements.",
        "",
        "Provides the session, the minigame that started, and the map it uses.",
        "",
        "Supports: Event trigger only (GET session, GET minigame, GET map)."
})
@Examples({
        "on game start:",
        "    broadcast \"%id of event-minigame% in session %id of event-session% has started\"",
        "on \"Bomberman\" game start:",
        "    broadcast \"Bomberman minigame started on map %id of event-gamemap%\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtGameStart extends SkriptEvent {
    private Literal<String> miniGameId;

    static {
        Skript.registerEvent("GameStart", EvtGameStart.class, GameStartEvent.class,
                "[%string%] [mini]game start",
                "[mini]game [%string%] start"
        );
        EventValues.registerEventValue(GameStartEvent.class, Session.class, GameStartEvent::getSession, EventValues.TIME_NOW);
        EventValues.registerEventValue(GameStartEvent.class, MiniGame.class, GameStartEvent::getMiniGame, EventValues.TIME_NOW);
        EventValues.registerEventValue(GameStartEvent.class, GameMap.class, GameStartEvent::getGameMap, EventValues.TIME_NOW);
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
        GameStartEvent event = (GameStartEvent) e;
        if (miniGameId == null) {
            return true;
        }
        String expectedId = miniGameId.getSingle(e);
        if (expectedId == null) {
            return true;
        }
        return expectedId.equalsIgnoreCase(event.getMiniGame().getId());
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        String id = (miniGameId == null || event == null) ? "any" : miniGameId.toString(event, b);
        return "on game " + id + " start";
    }
}
