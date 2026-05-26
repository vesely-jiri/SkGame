package cz.nox.skgame.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.core.game.quickplay.QuickplayQueue;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Is In Quickplay Queue")
@Description("Checks whether a player is currently waiting in the quickplay queue.")
@Examples({
        "if player is in quickplay queue:",
        "    send \"You are already searching for a game!\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class CondIsInQuickplayQueue extends Condition {

    private Expression<OfflinePlayer> playerExpr;

    static {
        Skript.registerCondition(CondIsInQuickplayQueue.class,
                "%offlineplayers% (is|are) in [the] quickplay queue",
                "%offlineplayers% (is not|are not|isn't|aren't) in [the] quickplay queue"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean kleenean,
                        SkriptParser.ParseResult parseResult) {
        playerExpr = (Expression<OfflinePlayer>) exprs[0];
        setNegated(matchedPattern == 1);
        return true;
    }

    @Override
    public boolean check(Event event) {
        QuickplayQueue queue = QuickplayQueue.getInstance();
        return playerExpr.check(event, p -> queue.isQueued(p.getUniqueId()), isNegated());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return playerExpr.toString(event, debug) + (isNegated() ? " is not" : " is") + " in quickplay queue";
    }
}
