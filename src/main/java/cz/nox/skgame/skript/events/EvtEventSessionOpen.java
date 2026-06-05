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
import cz.nox.skgame.api.game.event.EventSessionOpenEvent;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Server Event Session Open")
@Description({
        "Fires when an admin unlocks a server event session (visibility set to PUBLIC).",
        "Provides event-session and event-minigame (if set)."
})
@Examples({
        "on server event session open:",
        "    broadcast \"&6[Event] %minigame name of event-minigame% has opened!\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtEventSessionOpen extends SkriptEvent {

    static {
        Skript.registerEvent("EventSessionOpen", EvtEventSessionOpen.class, EventSessionOpenEvent.class,
                "([server] event [session] open[ed])"
        );
        EventValues.registerEventValue(EventSessionOpenEvent.class, Session.class, EventSessionOpenEvent::getSession, EventValues.TIME_NOW);
        EventValues.registerEventValue(EventSessionOpenEvent.class, MiniGame.class,
                e -> e.getSession().getMiniGame(), EventValues.TIME_NOW);
    }

    @Override
    public boolean init(Literal<?>[] literals, int i, SkriptParser.ParseResult parseResult) { return true; }

    @Override
    public boolean check(Event event) { return true; }

    @Override
    public String toString(@Nullable Event event, boolean b) { return "on server event session open"; }
}
