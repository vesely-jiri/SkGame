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
import cz.nox.skgame.api.game.event.LobbyEnterEvent;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Lobby Enter")
@Description({
        "Fires when a player enters the LOBBY role in a session.",
        "",
        "This includes both initial session join (null → LOBBY) and post-game return (PLAYER/SPECTATOR → LOBBY).",
        "This event is cancellable. Cancel it to prevent the player from entering the lobby.",
        "For first-join-only semantics use PlayerSessionJoinEvent.",
        "For post-game-return-only semantics use PlayerRoleChangeEvent with from=PLAYER/SPECTATOR.",
        "",
        "Optionally filter by minigame id (first string argument) and/or a specific session.",
        "",
        "Provides: event-player, event-session."
})
@Examples({
        "on lobby enter:",
        "    broadcast \"%name of event-player% entered the lobby of %id of event-session%\"",
        "",
        "on \"bomberman\" lobby enter:",
        "    send \"Welcome to the bomberman lobby!\" to event-player"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtLobbyEnter extends SkriptEvent {

    private Literal<String> miniGameId;
    private Literal<Session> sessionFilter;

    static {
        Skript.registerEvent("LobbyEnter", EvtLobbyEnter.class, LobbyEnterEvent.class,
                "[%string%] lobby enter [(in|of) %-session%]"
        );
        EventValues.registerEventValue(LobbyEnterEvent.class, Player.class,
                LobbyEnterEvent::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(LobbyEnterEvent.class, Session.class,
                LobbyEnterEvent::getSession, EventValues.TIME_NOW);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int i, SkriptParser.ParseResult parseResult) {
        if (args.length > 0 && args[0] != null) {
            this.miniGameId = (Literal<String>) args[0];
        }
        if (args.length > 1 && args[1] != null) {
            this.sessionFilter = (Literal<Session>) args[1];
        }
        return true;
    }

    @Override
    public boolean check(Event e) {
        LobbyEnterEvent event = (LobbyEnterEvent) e;
        if (miniGameId != null) {
            String expected = miniGameId.getSingle(e);
            if (expected != null) {
                var mg = event.getSession().getMiniGame();
                if (mg == null || !expected.equalsIgnoreCase(mg.getId())) return false;
            }
        }
        if (sessionFilter != null) {
            Session expected = sessionFilter.getSingle(e);
            if (expected != null && !expected.getId().equals(event.getSession().getId())) return false;
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "on lobby enter";
    }
}
