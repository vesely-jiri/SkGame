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
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Session - Player Team")
@Description({
        "The team name assigned to a player for their current session's game.",
        "Returns none (null) when the player has no session or no team is assigned.",
        "",
        "The 'skgame' prefix is mandatory to avoid collision with SkBee and vanilla",
        "Scoreboard team expressions — same reasoning as the mandatory 'session players' prefix.",
        "",
        "Supports: GET / SET / DELETE."
})
@Examples({
        "broadcast skgame team of player",
        "set skgame team of player to \"red\"",
        "delete skgame team of player",
        "if skgame team of player is \"blue\":"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprPlayerTeam extends SimpleExpression<String> {

    private Expression<Player> playerExpr;

    static {
        // COMBINED: "skgame" prefix disambiguates from vanilla team — multi-token by design
        Skript.registerExpression(ExprPlayerTeam.class, String.class, ExpressionType.COMBINED,
                "skgame team of %player%",
                "%player%'[s] skgame team"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean kleenean,
                        SkriptParser.ParseResult parseResult) {
        this.playerExpr = (Expression<Player>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable String[] get(Event event) {
        Player player = playerExpr.getSingle(event);
        if (player == null) return null;
        Session session = SessionManager.getInstance().getSession(player);
        if (session == null) return null;
        String team = session.getTeam(player);
        return team != null ? new String[]{team} : null;
    }

    @Override
    public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET           -> CollectionUtils.array(String.class);
            case DELETE, RESET -> CollectionUtils.array();
            default            -> null;
        };
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Player player = playerExpr.getSingle(event);
        if (player == null) return;
        Session session = SessionManager.getInstance().getSession(player);
        if (session == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                session.setTeam(player, (String) delta[0]);
            }
            case DELETE, RESET -> session.setTeam(player, null);
        }
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "skgame team of " + playerExpr.toString(event, debug);
    }
}
