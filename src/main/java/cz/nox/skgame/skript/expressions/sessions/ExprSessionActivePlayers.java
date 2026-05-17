package cz.nox.skgame.skript.expressions.sessions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Active Players")
@Description({
        "Returns all active (non-eliminated) players currently in a session.",
        "A player is considered eliminated when their game mode is set to spectator.",
        "This is a read-only expression."
})
@Examples({
        "set {_active::*} to playing players of {_session}",
        "loop active players of {_session}:",
        "    broadcast \"%loop-player% is still in the game!\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprSessionActivePlayers extends SimpleExpression<Player> {

    private Expression<Session> session;

    static {
        Skript.registerExpression(ExprSessionActivePlayers.class, Player.class, ExpressionType.PROPERTY,
                "[all] (playing|active) session players of %session%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Session>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable Player[] get(Event event) {
        Session session = this.session.getSingle(event);
        if (session == null) return new Player[0];
        return session.getPlayers().stream()
                .filter(p -> p.getGameMode() != GameMode.SPECTATOR)
                .toArray(Player[]::new);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends Player> getReturnType() {
        return Player.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "active players of " + session.toString(event, b);
    }
}
