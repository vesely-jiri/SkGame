package cz.nox.skgame.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import cz.nox.skgame.api.game.event.SessionCreateEvent;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class EvtSessionCreate extends SkriptEvent {

    private static final SessionManager sessionManager = SessionManager.getInstance();

    static {
        Skript.registerEvent("SessionCreateDisband", EvtSessionCreate.class, SessionCreateEvent.class,
                "session create"
        );
        EventValues.registerEventValue(SessionCreateEvent.class, Session.class, SessionCreateEvent::getSession, EventValues.TIME_NOW);
    }

    @Override
    public boolean init(Literal<?>[] literals, int i, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "on session create";
    }
}
