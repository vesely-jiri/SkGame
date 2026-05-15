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
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.gui.event.MapsGuiOpenEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Maps GUI Open")
@Description("Fires when a player opens the map selection GUI. Cancellable. Provides the session and current minigame.")
@Examples({
        "on maps gui opening:",
        "    cancel event # lock map selection"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtMapsGuiOpen extends SkriptEvent {

    static {
        Skript.registerEvent("MapsGuiOpen", EvtMapsGuiOpen.class, MapsGuiOpenEvent.class,
                "maps gui open[ing]");
        EventValues.registerEventValue(MapsGuiOpenEvent.class, Player.class,
                MapsGuiOpenEvent::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(MapsGuiOpenEvent.class, Session.class,
                MapsGuiOpenEvent::getSession, EventValues.TIME_NOW);
        EventValues.registerEventValue(MapsGuiOpenEvent.class, MiniGame.class,
                MapsGuiOpenEvent::getMiniGame, EventValues.TIME_NOW);
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
        return "maps gui opening";
    }
}
