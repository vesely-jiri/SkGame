package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.SkGame;
import cz.nox.skgame.core.locale.ScriptLocaleRegistry;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Send Locale Message")
@Description({
        "Sends a localized message defined via 'locale \"ns\":' to one or more players.",
        "The key format is \"namespace:message.key\".",
        "Each player receives the message in their own locale.",
        "No-op if the key is not found; logs a warning once per missing key.",
})
@Examples({
        "# Send to a single player — message in their locale",
        "send locale \"koth:game.start\" to event-player",
        "",
        "# Send to all players in session — each gets their own locale",
        "send locale \"bomberman:game.start\" to session players of event-session",
        "",
        "# Send to all members (players + spectators)",
        "send locale \"koth:game.over\" to session members of event-session"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSendLocale extends Effect {

    private Expression<String> key;
    private Expression<Player> players;
    @Nullable private Expression<Object> args;

    static {
        Skript.registerEffect(EffSendLocale.class,
                "send locale %string% to %players% [with [arguments] %objects%]"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.key = (Expression<String>) exprs[0];
        this.players = (Expression<Player>) exprs[1];
        this.args = (Expression<Object>) exprs[2];
        return true;
    }

    @Override
    protected void execute(Event event) {
        String fullKey = key.getSingle(event);
        if (fullKey == null) return;

        int colon = fullKey.indexOf(':');
        if (colon < 0) {
            SkGame.getInstance().getLogger().warning("[SkGame/ScriptLocale] send locale: missing namespace separator in '" + fullKey + "'");
            return;
        }
        String namespace = fullKey.substring(0, colon);
        String messageKey = fullKey.substring(colon + 1);
        Object[] argsArray = this.args != null ? this.args.getAll(event) : new Object[0];

        ScriptLocaleRegistry registry = ScriptLocaleRegistry.getInstance();
        for (Player player : players.getArray(event)) {
            String text = registry.get(namespace, messageKey, player, argsArray);
            if (text == null) {
                SkGame.getInstance().getLogger().warning("[SkGame/ScriptLocale] missing key: " + fullKey);
                continue;
            }
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "send locale " + key.toString(event, b) + " to " + players.toString(event, b);
    }
}
