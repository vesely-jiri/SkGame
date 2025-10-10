package cz.nox.skgame.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.registrations.EventValues;
import cz.nox.skgame.api.game.event.GamePlayerSessionLeave;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

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
