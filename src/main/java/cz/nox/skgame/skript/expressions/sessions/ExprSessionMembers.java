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

@Name("Session - Members")
@Description({
        "Returns all members of a session: both players and spectators combined.",
        "Read-only snapshot — modifying the returned set does not affect the session.",
})
@Examples({
        "loop members of {_session}:",
        "    send \"Hello %loop-player%!\" to loop-player"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprSessionMembers extends SimpleExpression<Player> {

    private Expression<Session> session;

    static {
        Skript.registerExpression(ExprSessionMembers.class, Player.class, ExpressionType.PROPERTY,
                "[all] session members of %session%"
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
        if (session == null) return null;
        return session.getMembers().toArray(new Player[0]);
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
        return "members of " + this.session.toString(e, b);
    }
}
