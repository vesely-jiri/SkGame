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
import cz.nox.skgame.api.game.event.PlayerScoreChangeEvent;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Game Score Change")
@Description({
        "Fires when a player's score changes in a session via the score expression.",
        "",
        "event-number    = new score (TIME_NOW)",
        "past event-number = old score (TIME_PAST)",
        "delta = event-number - past event-number (may be negative for REMOVE)",
        "",
        "Fires for SET, ADD, REMOVE, and DELETE/RESET (new=0).",
        "Does NOT fire for internal lifecycle resets (temp value cleanup at game end).",
        "",
        "Supports: Event trigger only (GET session, GET minigame, GET player, GET score via event-number)."
})
@Examples({
        "# event-player, event-session, event-number (new score), past event-number (old score)",
        "on game score change:",
        "    broadcast \"%name of event-player%: %past event-number% -> %event-number%\"",
        "",
        "# Filter by minigame and check win condition",
        "on \"koth\" game score changed:",
        "    if event-number >= 100:",
        "        stop game of event-session with reason \"win\"",
        "",
        "# Update scoreboard on score change",
        "on game score change:",
        "    set score scoreboard line 1 of event-player to \"Score: %event-number%\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtScoreChange extends SkriptEvent {
    private Literal<String> miniGameId;

    static {
        Skript.registerEvent("ScoreChange", EvtScoreChange.class, PlayerScoreChangeEvent.class,
                "[%string%] [mini]game score change[d]",
                "[mini]game [%string%] score change[d]"
        );
        EventValues.registerEventValue(PlayerScoreChangeEvent.class, Player.class,
                PlayerScoreChangeEvent::getPlayer, EventValues.TIME_NOW);
        EventValues.registerEventValue(PlayerScoreChangeEvent.class, Session.class,
                PlayerScoreChangeEvent::getSession, EventValues.TIME_NOW);
        EventValues.registerEventValue(PlayerScoreChangeEvent.class, MiniGame.class,
                PlayerScoreChangeEvent::getMiniGame, EventValues.TIME_NOW);
        // new score: event-number; old score: past event-number
        EventValues.registerEventValue(PlayerScoreChangeEvent.class, Number.class,
                e -> e.getNewScore(), EventValues.TIME_NOW);
        EventValues.registerEventValue(PlayerScoreChangeEvent.class, Number.class,
                e -> e.getOldScore(), EventValues.TIME_PAST);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int i, SkriptParser.ParseResult parseResult) {
        if (args.length > 0 && args[0] != null) {
            this.miniGameId = (Literal<String>) args[0];
        }
        return true;
    }

    @Override
    public boolean check(Event e) {
        PlayerScoreChangeEvent event = (PlayerScoreChangeEvent) e;
        if (miniGameId == null) return true;
        String expected = miniGameId.getSingle(e);
        if (expected == null) return true;
        MiniGame mg = event.getMiniGame();
        return mg != null && expected.equalsIgnoreCase(mg.getId());
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        String id = (miniGameId == null || event == null) ? "any" : miniGameId.toString(event, b);
        return "on " + id + " game score changed";
    }
}
