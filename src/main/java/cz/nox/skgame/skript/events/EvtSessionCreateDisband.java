package cz.nox.skgame.skript.events;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import cz.nox.skgame.api.game.event.SessionCreateEvent;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class EvtSessionCreateDisband extends SkriptEvent {

    private static final SessionManager sessionManager = SessionManager.getInstance();

    static {
        Skript.registerEvent("sessionCreate", EvtSessionCreateDisband.class, SessionCreateEvent.class,
                "session (create|1:(disband|delete))"
        );
    }

    @Override
    public boolean init(Literal<?>[] literals, int i, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    public boolean check(Event event) {
        return false;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return null;
    }
}
