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
import cz.nox.skgame.api.gui.event.AdminGuiOpenEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Admin GUI Open")
@Description("Fires when a player opens the admin GUI. Cancellable.")
@Examples({
        "on admin gui opening:",
        "    broadcast \"%player% opened the admin panel\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtAdminGuiOpen extends SkriptEvent {

    static {
        Skript.registerEvent("AdminGuiOpen", EvtAdminGuiOpen.class, AdminGuiOpenEvent.class,
                "admin gui open[ing]");
        EventValues.registerEventValue(AdminGuiOpenEvent.class, Player.class,
                AdminGuiOpenEvent::getPlayer, EventValues.TIME_NOW);
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
        return "admin gui opening";
    }
}
