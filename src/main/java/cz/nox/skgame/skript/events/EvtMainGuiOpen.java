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
import cz.nox.skgame.api.gui.event.MainGuiOpenEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Main GUI Open")
@Description("Fires when a player opens the main game GUI. Cancellable — cancel to prevent the GUI from opening.")
@Examples({
        "on main gui opening:",
        "    if player has permission \"vip.only\":",
        "        cancel event"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtMainGuiOpen extends SkriptEvent {

    static {
        Skript.registerEvent("MainGuiOpen", EvtMainGuiOpen.class, MainGuiOpenEvent.class,
                "main gui open[ing]");
        EventValues.registerEventValue(MainGuiOpenEvent.class, Player.class,
                MainGuiOpenEvent::getPlayer, EventValues.TIME_NOW);
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
        return "main gui opening";
    }
}
