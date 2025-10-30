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
import cz.nox.skgame.api.game.event.GamePlayerSessionJoin;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Player Session Join")
@Description({
        "Fires when a player joins a session.",
        "",
        "Useful for initializing player data, sending welcome messages, or setting up session-specific properties.",
        "",
        "Provides the player who joined and the session they joined.",
        "",
        "Supports: Event trigger only (GET player, GET session)."
})
@Examples({
        "on player session join:",
        "    broadcast \"%name of event-player% joined session %id of event-session%\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtPlayerSessionJoin extends SkriptEvent {

    static {
        Skript.registerEvent("PlayerSessionJoin", EvtPlayerSessionJoin.class, GamePlayerSessionJoin.class,
                "player session join"
        );
        EventValues.registerEventValue(GamePlayerSessionJoin.class, Player.class, GamePlayerSessionJoin::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(GamePlayerSessionJoin.class, Session.class, GamePlayerSessionJoin::getSession, EventValues.TIME_NOW);
    }

    @Override
    public boolean init(Literal<?>[] literals, int i, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event e) {
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "on player session join";
    }
}
