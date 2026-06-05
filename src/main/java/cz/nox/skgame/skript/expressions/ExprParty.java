package cz.nox.skgame.skript.expressions;

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

@Name("Party of Session")
@Description("Returns all lobby members (players with role LOBBY) of a session.")
@Examples({
        "loop party of event-session:",
        "    send \"Hello %loop-player%\" to loop-player"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprParty extends SimpleExpression<Player> {

    private Expression<Object> session;

    static {
        Skript.registerExpression(ExprParty.class, Player.class, ExpressionType.PROPERTY,
                "[the] [session] party of %object%"
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
        if (!(this.session.getSingle(event) instanceof Session s)) return new Player[0];
        return s.getLobbyMembers().toArray(new Player[0]);
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
        return "party of " + this.session.toString(event, b);
    }
}
