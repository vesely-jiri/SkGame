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
import cz.nox.skgame.api.game.event.PlayerRoleChangeEvent;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionRole;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Player Promoted")
@Description({
        "Fires after a session member has been promoted from spectator to active player (SPECTATOR → PLAYER).",
        "",
        "Not cancellable — the role change has already occurred. Use this event to react:",
        "give starting equipment, update scoreboards, or announce the promotion.",
        "",
        "Optionally filter by minigame id and/or a specific session.",
        "",
        "Provides: event-player, event-session, event-minigame, event-gamemap."
})
@Examples({
        "on player promoted:",
        "    send \"You joined the game!\" to event-player",
        "",
        "on \"koth\" player promoted in event-session:",
        "    give event-player a diamond sword"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtPlayerPromoted extends SkriptEvent {

    private Literal<String> miniGameId;
    private Literal<Session> sessionFilter;

    static {
        Skript.registerEvent("PlayerPromoted", EvtPlayerPromoted.class, PlayerRoleChangeEvent.class,
                "[%string%] [player] promoted [to player] [(in|of) %-session%]"
        );
        EventValues.registerEventValue(PlayerRoleChangeEvent.class, Player.class,
                PlayerRoleChangeEvent::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(PlayerRoleChangeEvent.class, Session.class,
                PlayerRoleChangeEvent::getSession, EventValues.TIME_NOW);
        EventValues.registerEventValue(PlayerRoleChangeEvent.class, MiniGame.class,
                e -> e.getSession().getMiniGame(), EventValues.TIME_NOW);
        EventValues.registerEventValue(PlayerRoleChangeEvent.class, GameMap.class,
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
        PlayerRoleChangeEvent event = (PlayerRoleChangeEvent) e;
        // Only fires for SPECTATOR → PLAYER direction
        if (event.getFrom() != SessionRole.SPECTATOR || event.getTo() != SessionRole.PLAYER) {
            return false;
        }
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
        return "on player promoted";
    }
}
