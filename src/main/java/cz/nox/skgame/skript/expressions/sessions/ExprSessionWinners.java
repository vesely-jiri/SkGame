package cz.nox.skgame.skript.expressions.sessions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
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
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@SuppressWarnings("unused")
@Name("Session - Winners")
@Description({
        "The declared winners of this session's current (or last completed) game.",
        "Set by minigame scripts during 'on game stop' before endGame broadcasts them.",
        "Automatically cleared at the start of each game/round.",
        "",
        "Supports: GET / SET / RESET / DELETE."
})
@Examples({
        "on game stop:",
        "    set winners of event-session to {_lastSurvivor}",
        "",
        "on game stop:",
        "    set winners of event-session to {_team::*}",
        "",
        "loop winners of {_session}:",
        "    give loop-player a diamond"
})
@Since("1.0.0")
public class ExprSessionWinners extends SimpleExpression<Player> {

    private Expression<Session> session;

    static {
        Skript.registerExpression(ExprSessionWinners.class, Player.class, ExpressionType.PROPERTY,
                "winners of %session%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.session = (Expression<Session>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable Player[] get(Event event) {
        Session s = session.getSingle(event);
        if (s == null) return null;
        return s.getWinners().toArray(new Player[0]);
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> CollectionUtils.array(Player[].class);
            case RESET, DELETE -> CollectionUtils.array();
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Session s = session.getSingle(event);
        if (s == null) return;
        switch (mode) {
            case SET -> {
                Player[] players = delta == null ? new Player[0]
                        : Arrays.copyOf(delta, delta.length, Player[].class);
                s.setWinners(Arrays.asList(players));
            }
            case RESET, DELETE -> s.clearWinners();
        }
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
        return "winners of " + session.toString(e, b);
    }
}
