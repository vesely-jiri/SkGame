package cz.nox.skgame.skript.expressions.gameplayers;

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
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("GamePlayer - Session")
@Description({
        "Returns the session in which a given player is currently participating."
})
@Examples({
        "set {_session} to session of player",
        "broadcast \"Player %player% is in session %{_session}%\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprGamePlayerSession extends SimpleExpression<Session> {
    private static final SessionManager sessionManager = SessionManager.getInstance();

    private Expression<Player> player;
    private boolean all;

    static {
        Skript.registerExpression(ExprGamePlayerSession.class, Session.class, ExpressionType.SIMPLE,
                "session[list:s] of %player%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.player = (Expression<Player>) exprs[0];
        this.all = parseResult.hasTag("list");
        return true;
    }

    @Override
    protected Session @Nullable [] get(Event event) {
        Player player = this.player.getSingle(event);
        if (player == null) return null;
        if (all) {
            return sessionManager.getSessions(player);
        }
        return CollectionUtils.array(sessionManager.getSession(player));
    }

    @Override
    public boolean isSingle() {
        return !all;
    }

    @Override
    public Class<? extends Session> getReturnType() {
        return Session.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        return "session"
                + (all ? "s " : " ")
                + "of player "
                + this.player.toString(e,b);
    }
}
