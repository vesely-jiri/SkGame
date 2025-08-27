package cz.nox.skgame.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import cz.nox.skgame.api.game.event.GameStopEvent;
import cz.nox.skgame.api.game.model.GameMode;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

/**
 * Called when gamemode of session stops
 */
@SuppressWarnings("unused")
public class EvtGameStop extends SkriptEvent {
    private Session session;
    private Literal<GameMode> gamemode;

    static {
        Skript.registerEvent("GameStop", EvtGameStop.class, GameStopEvent.class,
                "[%string%] game stop",
                "game [%string%] stop"
        );
        EventValues.registerEventValue(GameStopEvent.class, Session.class, GameStopEvent::getSession, EventValues.TIME_NOW);
        EventValues.registerEventValue(GameStopEvent.class, GameMode.class, GameStopEvent::getGameMode, EventValues.TIME_NOW);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int i, SkriptParser.ParseResult parseResult) {
        if (args[0] != null) {
            this.gamemode = (Literal<GameMode>) args[0];
        }
        return true;
    }

    @Override
    public boolean check(Event e) {
        return this.gamemode.getSingle(e) == ((GameStopEvent) e).getGameMode();
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return null;
    }
}
