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
import cz.nox.skgame.api.game.event.SessionSettingsChangedEvent;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session Settings Change")
@Description({
        "Fires whenever a host changes a session setting from the GUI or a Skript expression.",
        "",
        "Covered settings: minigame, map, rounds, visibility, shuffle, allow-spectate.",
        "Use event-string to branch on which setting changed.",
        "",
        "Does NOT fire during internal lifecycle mutations (vote-tally map selection,",
        "auto-next-round, disband cleanup). Only fires from direct user action.",
        "",
        "Supports: Event trigger only (GET session, GET key as event-string)."
})
@Examples({
        "on session settings change:",
        "    broadcast \"Session %id of event-session% changed: %event-string%\"",
        "on session settings changed:",
        "    if event-string is \"minigame\":",
        "        broadcast \"Minigame changed to %minigame of event-session%\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtSessionSettingsChanged extends SkriptEvent {

    static {
        Skript.registerEvent("SessionSettingsChanged", EvtSessionSettingsChanged.class,
                SessionSettingsChangedEvent.class,
                "session [settings] change[d]"
        );
        EventValues.registerEventValue(SessionSettingsChangedEvent.class, Session.class,
                SessionSettingsChangedEvent::getSession, EventValues.TIME_NOW);
        EventValues.registerEventValue(SessionSettingsChangedEvent.class, String.class,
                SessionSettingsChangedEvent::getKey, EventValues.TIME_NOW);
    }

    @Override
    public boolean init(Literal<?>[] args, int i, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event e) {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "on session settings changed";
    }
}
