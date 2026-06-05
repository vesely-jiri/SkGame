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
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
@Name("Session - Shuffled Players")
@Description({
        "Returns session players in random order when session shuffle is enabled, otherwise in default order.",
        "Use at game start to randomize spawn assignments.",
        "",
        "Supports: GET only."
})
@Examples({
        "loop shuffled session players of event-session:",
        "    teleport loop-player to {_spawn::%loop-iteration%}"
})
@Since("1.0.0")
public class ExprShuffledSessionPlayers extends SimpleExpression<Player> {

    private Expression<Object> session;

    static {
        Skript.registerExpression(ExprShuffledSessionPlayers.class, Player.class, ExpressionType.PROPERTY,
                "shuffled session players of %object%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Object>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable Player[] get(Event event) {
        if (!(this.session.getSingle(event) instanceof Session s)) return null;
        List<Player> players = new ArrayList<>(s.getPlayers());
        if (s.isShuffle()) Collections.shuffle(players);
        return players.toArray(new Player[0]);
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
    public String toString(@Nullable Event e, boolean b) {
        return "shuffled session players of " + this.session.toString(e, b);
    }
}
