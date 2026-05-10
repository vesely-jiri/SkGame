package cz.nox.skgame.skript.messages;

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
import cz.nox.skgame.api.messages.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Messages - Localized Message String")
@Description({
        "Returns a localized message string for a given key.",
        "If 'for %player%' is omitted, the fallback locale (en_US) is used.",
        "Optional positional arguments ({0}, {1}, …) are substituted into the message.",
        "",
        "Supports: GET only."
})
@Examples({
        "set {_msg} to skgame message \"session.created\" for player",
        "broadcast skgame message \"session.full\" with arguments 4 and 8",
        "set {_title} to skgame message \"admin.wand.given\" for player with arguments player's name"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprMessage extends SimpleExpression<String> {

    static {
        Skript.registerExpression(ExprMessage.class, String.class, ExpressionType.SIMPLE,
                "skgame message %string% [for %-player%] [with [arguments] %objects%]"
        );
    }

    private Expression<String> key;
    @Nullable private Expression<Player> player;
    @Nullable private Expression<Object> args;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        key    = (Expression<String>) exprs[0];
        player = exprs[1] != null ? (Expression<Player>) exprs[1] : null;
        args   = exprs[2] != null ? (Expression<Object>) exprs[2] : null;
        return true;
    }

    @Override
    protected @Nullable String[] get(Event event) {
        String k = key.getSingle(event);
        if (k == null) return new String[0];
        Player p = player != null ? player.getSingle(event) : null;
        Object[] argValues = args != null ? args.getAll(event) : new Object[0];
        return new String[]{Messages.get(k, p, argValues)};
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
        return "skgame message " + key.toString(event, debug)
                + (player != null ? " for " + player.toString(event, debug) : "")
                + (args != null ? " with arguments " + args.toString(event, debug) : "");
    }
}
