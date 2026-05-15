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
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.gui.event.SessionGuiOpenEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session GUI Open")
@Description("Fires when a player opens the session lobby GUI. Cancellable.")
@Examples({
        "on session gui opening:",
        "    broadcast \"%player% is viewing session %event-session%\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtSessionGuiOpen extends SkriptEvent {

    static {
        Skript.registerEvent("SessionGuiOpen", EvtSessionGuiOpen.class, SessionGuiOpenEvent.class,
                "session gui open[ing]");
        EventValues.registerEventValue(SessionGuiOpenEvent.class, Player.class,
                SessionGuiOpenEvent::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(SessionGuiOpenEvent.class, Session.class,
                SessionGuiOpenEvent::getSession, EventValues.TIME_NOW);
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
        return "session gui opening";
    }
}
