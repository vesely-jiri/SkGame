package cz.nox.skgame.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.model.GameMode;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Called when gamemode of session starts
 */
@SuppressWarnings("unused")
public class    EvtGameStart extends SkriptEvent {
    private Session session;
    private Literal<GameMode> gameMode;

    static {
        Skript.registerEvent("GameStart", EvtGameStart.class, GameStartEvent.class,
                "[%string%] game start",
                "game [%string%] start"
        );
        EventValues.registerEventValue(GameStartEvent.class, Session.class, GameStartEvent::getSession, EventValues.TIME_NOW);
        EventValues.registerEventValue(GameStartEvent.class, GameMode.class, GameStartEvent::getGameMode, EventValues.TIME_NOW);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int i, SkriptParser.ParseResult parseResult) {
        if (args[0] != null) {
            this.gameMode = (Literal<GameMode>) args[0];
        }
        return true;
    }

    @Override
    public boolean check(Event e) {
        return this.gameMode.getSingle(e) == ((GameStartEvent) e).getGameMode();
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "on game " + gameMode.getSingle(event) + " start of session " + session;
    }
}
