package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Stop Session Game")
@Description({
        "Stops the game running in a specific session.",
        "",
        "You can optionally provide a reason for the game stop.",
        "Removes temp session and player values, fires GameStopEvent, then returns all members to LOBBY role.",
        "Can't be cancelled."
})
@Examples({
        "stop game of {_session}",
        "stop game of {_session} with reason \"no_players\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSessionGameStop extends Effect {

    private Expression<Session> session;
    private @Nullable Expression<String> reason;

    static {
        Skript.registerEffect(EffSessionGameStop.class,
                "stop game of %session% [with reason %-string%]"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Session>) exprs[0];
        if (exprs[1] != null) {
            this.reason = (Expression<String>) exprs[1];
        }
        return true;
    }

    @Override
    protected void execute(Event e) {
        Session session = this.session.getSingle(e);
        if (session == null) return;
        String stopReason = (this.reason != null) ? this.reason.getSingle(e) : null;
        SessionLifecycleManagerImpl.getInstance().endGame(session, stopReason != null ? stopReason : "default");
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "stop game of " + this.session.toString(event, b);
    }
}
