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
import cz.nox.skgame.api.game.event.SpectatorJoinEvent;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Spectator Join")
@Description({
        "Fires when a player attempts to join a session as a spectator.",
        "",
        "This event is cancellable. Cancel it to prevent the player from spectating.",
        "",
        "Optionally filter by minigame id (first string argument) and/or a specific session.",
        "",
        "Provides: event-player, event-session, event-minigame, event-gamemap."
})
@Examples({
        "# event-player, event-session, event-minigame, event-gamemap available",
        "on spectator join:",
        "    broadcast \"%name of event-player% is now spectating %event-minigame% on %event-gamemap%\"",
        "    set gamemode of event-player to spectator",
        "",
        "# Filter by minigame — block spectators when game not running",
        "on \"bomberman\" spectator join:",
        "    if state of event-session is not started:",
        "        cancel event",
        "        send \"Can only spectate a running game!\" to event-player"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtSpectatorJoin extends SkriptEvent {

    private Literal<String> miniGameId;
    private Literal<Session> sessionFilter;

    static {
        Skript.registerEvent("SpectatorJoin", EvtSpectatorJoin.class, SpectatorJoinEvent.class,
                "[%string%] spectator join [(in|of) %-session%]"
        );
        EventValues.registerEventValue(SpectatorJoinEvent.class, Player.class,
                SpectatorJoinEvent::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(SpectatorJoinEvent.class, Session.class,
                SpectatorJoinEvent::getSession, EventValues.TIME_NOW);
        EventValues.registerEventValue(SpectatorJoinEvent.class, MiniGame.class,
                e -> e.getSession().getMiniGame(), EventValues.TIME_NOW);
        EventValues.registerEventValue(SpectatorJoinEvent.class, GameMap.class,
                e -> e.getSession().getGameMap(), EventValues.TIME_NOW);
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
        SpectatorJoinEvent event = (SpectatorJoinEvent) e;
        if (miniGameId != null) {
            String expected = miniGameId.getSingle(e);
            if (expected != null && !expected.equalsIgnoreCase(event.getSession().getMiniGame().getId())) {
                return false;
            }
        }
        if (sessionFilter != null) {
            Session expected = sessionFilter.getSingle(e);
            if (expected != null && !expected.getId().equals(event.getSession().getId())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "on spectator join";
    }
}
