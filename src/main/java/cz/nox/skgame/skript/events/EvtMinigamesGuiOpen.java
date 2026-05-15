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
import cz.nox.skgame.api.gui.event.MinigamesGuiOpenEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Minigames GUI Open")
@Description("Fires when a player opens the minigame selection GUI. Cancellable.")
@Examples({
        "on minigames gui opening:",
        "    cancel event # lock minigame selection"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtMinigamesGuiOpen extends SkriptEvent {

    static {
        Skript.registerEvent("MinigamesGuiOpen", EvtMinigamesGuiOpen.class, MinigamesGuiOpenEvent.class,
                "minigames gui open[ing]");
        EventValues.registerEventValue(MinigamesGuiOpenEvent.class, Player.class,
                MinigamesGuiOpenEvent::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(MinigamesGuiOpenEvent.class, Session.class,
                MinigamesGuiOpenEvent::getSession, EventValues.TIME_NOW);
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
        return "minigames gui opening";
    }
}
