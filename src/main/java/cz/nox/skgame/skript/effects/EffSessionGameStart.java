package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.Timespan;
import ch.njol.skript.util.Timespan.TimePeriod;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.GameStartReason;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Start Session Game")
@Description({
        "Starts the game assigned to a specific session.",
        "Optionally provide a countdown delay — the session enters STARTING state during the delay.",
        "",
        "Only works if the session is in LOBBY state and the session's map supports the MiniGame.",
        "Triggers a GameStartEvent when the game actually begins.",
        "Transitions all LOBBY members to PLAYER role.",
        "",
        "Supports: EXECUTE only."
})
@Examples({
        "start game of {_session}",
        "start game of {_session} in 10 seconds"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSessionGameStart extends Effect {

    private Expression<Session> session;
    private @Nullable Expression<Timespan> delay;

    static {
        Skript.registerEffect(EffSessionGameStart.class,
                "start game of %session% [in %-timespan%]"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Session>) exprs[0];
        this.delay = (Expression<Timespan>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Session session = this.session.getSingle(event);
        if (session == null) return;

        Long ticks = null;
        if (this.delay != null) {
            Timespan timespan = this.delay.getSingle(event);
            if (timespan != null) ticks = timespan.getAs(TimePeriod.TICK);
        }

        SessionLifecycleManagerImpl.getInstance().startGame(session, GameStartReason.HOST_START, ticks);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "start game of " + this.session.toString(event, b);
    }
}
