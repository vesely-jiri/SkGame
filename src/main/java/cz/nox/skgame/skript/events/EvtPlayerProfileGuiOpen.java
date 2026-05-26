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
import cz.nox.skgame.api.gui.event.PlayerProfileGuiOpenEvent;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Player Profile GUI Open")
@Description("Fires when a player opens another player's profile GUI. Cancellable — cancel to prevent the GUI from opening.")
@Examples({
        "on player profile gui opening:",
        "    cancel event"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtPlayerProfileGuiOpen extends SkriptEvent {

    static {
        Skript.registerEvent("PlayerProfileGuiOpen", EvtPlayerProfileGuiOpen.class,
                PlayerProfileGuiOpenEvent.class, "[player] profile [gui] open[ing]");
        EventValues.registerEventValue(PlayerProfileGuiOpenEvent.class, Player.class,
                PlayerProfileGuiOpenEvent::getViewer, EventValues.TIME_NOW);
        EventValues.registerEventValue(PlayerProfileGuiOpenEvent.class, OfflinePlayer.class,
                PlayerProfileGuiOpenEvent::getSubject, EventValues.TIME_NOW);
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
        return "player profile gui opening";
    }
}
