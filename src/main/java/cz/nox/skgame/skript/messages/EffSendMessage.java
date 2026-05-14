package cz.nox.skgame.skript.messages;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.messages.Messages;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Messages - Send Localized Message")
@Description({
        "Send a localized message to one or more players.",
        "The message key is looked up in the player's locale (or en_US fallback).",
        "Optional positional arguments ({0}, {1}, …) are substituted into the message.",
        "",
        "Supports: EXECUTE only."
})
@Examples({
        "send skgame message \"session.joined\" to player",
        "send skgame message \"session.full\" to player with arguments 3 and 10",
        "send skgame message \"errors.invalid-input\" to all players with arguments \"blue\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSendMessage extends Effect {

    static {
        Skript.registerEffect(EffSendMessage.class,
                "send skgame message %string% to %players% [with [arguments] %-objects%]"
        );
    }

    private Expression<String> key;
    private Expression<Player> players;
    @Nullable private Expression<Object> args;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        key     = (Expression<String>) exprs[0];
        players = (Expression<Player>) exprs[1];
        args    = exprs[2] != null ? (Expression<Object>) exprs[2] : null;
        return true;
    }

    @Override
    protected void execute(Event event) {
        String k = key.getSingle(event);
        if (k == null) return;
        Object[] argValues = args != null ? args.getAll(event) : new Object[0];
        for (Player player : players.getArray(event)) {
            Messages.send(player, k, argValues);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "send skgame message " + key.toString(event, debug)
                + " to " + players.toString(event, debug)
                + (args != null ? " with arguments " + args.toString(event, debug) : "");
    }
}
