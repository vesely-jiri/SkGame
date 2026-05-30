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
import cz.nox.skgame.api.game.event.RoundEndEvent;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Game Round End")
@Description({
        "Fires when a round of a multi-round session completes naturally.",
        "",
        "Not fired for forced teardown stops (disband, shutdown, abandoned, admin force-end,",
        "minigame-disabled, no-players). Use this event to display per-round results or run",
        "inter-round cleanup.",
        "",
        "At the fire point, event-session's current round still reflects the round that just ended.",
        "Use `current round of event-session < session rounds of event-session` to check if more",
        "rounds remain instead of event-boolean (hasNextRound is not exposed as an event-value).",
        "",
        "Supports: Event trigger only (GET session, GET minigame, GET round number, GET total rounds)."
})
@Examples({
        "on game round end:",
        "    broadcast \"Round %current round of event-session% of %session rounds of event-session% finished\"",
        "on \"Bomberman\" game round end:",
        "    broadcast \"Bomberman round ended in session %id of event-session%\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EvtRoundEnd extends SkriptEvent {
    private Literal<String> miniGameId;

    static {
        Skript.registerEvent("RoundEnd", EvtRoundEnd.class, RoundEndEvent.class,
                "[%string%] [mini]game round end",
                "[mini]game [%string%] round end"
        );
        EventValues.registerEventValue(RoundEndEvent.class, Session.class, RoundEndEvent::getSession, EventValues.TIME_NOW);
        EventValues.registerEventValue(RoundEndEvent.class, MiniGame.class, RoundEndEvent::getMiniGame, EventValues.TIME_NOW);
        // event-number = round that just completed (1-based). Total rounds: use `session rounds of event-session`.
        EventValues.registerEventValue(RoundEndEvent.class, Number.class, e -> e.getRound(), EventValues.TIME_NOW);
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
        RoundEndEvent event = (RoundEndEvent) e;
        if (miniGameId == null) return true;
        String expected = miniGameId.getSingle(e);
        if (expected == null) return true;
        MiniGame mg = event.getMiniGame();
        return mg != null && expected.equalsIgnoreCase(mg.getId());
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        String id = (miniGameId == null || event == null) ? "any" : miniGameId.toString(event, b);
        return "on " + id + " game round end";
    }
}
