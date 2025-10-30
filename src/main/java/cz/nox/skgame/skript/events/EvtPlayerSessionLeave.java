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
import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Player Session Leave")
@Description({
        "Fires when a player leaves a session.",
        "",
        "Useful for tracking player exits, cleaning up player data, or sending messages to other players in the session.",
        "",
        "Provides the player who left and the session they left from.",
        "",
        "Supports: Event trigger only (GET player, GET session)."
})
@Examples({
        "on player session leave:",
        "    broadcast \"%name of event-player% has left session %id of event-session%\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtPlayerSessionLeave extends SkriptEvent {

    static {
        Skript.registerEvent("PlayerSessionLeave", EvtPlayerSessionLeave.class, GamePlayerSessionLeave.class,
                "player session leave"
        );
        EventValues.registerEventValue(GamePlayerSessionLeave.class, Player.class,GamePlayerSessionLeave::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(GamePlayerSessionLeave.class, Session.class, GamePlayerSessionLeave::getSession, EventValues.TIME_NOW);
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
        return "on player session leave";
    }
}
