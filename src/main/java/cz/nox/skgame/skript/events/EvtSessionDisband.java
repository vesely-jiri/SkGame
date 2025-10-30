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
import cz.nox.skgame.api.game.event.SessionDisbandEvent;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Session Disband")
@Description({
        "Fires when a game session is disbanded or deleted.",
        "",
        "You can use this event to clean up resources, notify players, or perform any custom actions when a session ends.",
        "",
        "Provides the session that was disbanded.",
})
@Examples({
        "on session disband:",
        "    broadcast \"Session %id of event-session% has ended.\"",
        "    clear players of event-session"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtSessionDisband extends SkriptEvent {

    private static final SessionManager sessionManager = SessionManager.getInstance();

    static {
        Skript.registerEvent("SessionDisband", EvtSessionDisband.class, SessionDisbandEvent.class,
                "session (disband|delete)"
        );
        EventValues.registerEventValue(SessionDisbandEvent.class, Session.class, SessionDisbandEvent::getSession, EventValues.TIME_NOW);
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
        return "on session disband";
    }
}
