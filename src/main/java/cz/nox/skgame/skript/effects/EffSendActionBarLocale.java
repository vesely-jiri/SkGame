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

@Name("Send Action Bar Locale")
@Description({
        "Sends a localized action bar message to one or more players using their own locale.",
        "The key format is \"namespace:message.key\".",
})
@Examples({
        "# Action bar to a single player",
        "send actionbar locale \"koth:time.left\" to event-player",
        "",
        "# Action bar to all session players — useful for countdowns",
        "send actionbar locale \"bomberman:countdown\" to session players of event-session",
        "",
        "# Periodically update action bar in a repeating task",
        "every 5 seconds:",
        "    loop all sessions:",
        "        if state of loop-session is started:",
        "            send actionbar locale \"koth:score.update\" to session players of loop-session"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSendActionBarLocale extends Effect {

    private Expression<String> key;
    private Expression<Player> players;
    @Nullable private Expression<Object> args;

    static {
        Skript.registerEffect(EffSendActionBarLocale.class,
                "send actionbar locale %string% to %players% [with [arguments] %-objects%]"
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
            SkGame.getInstance().getLogger().warning("[SkGame/ScriptLocale] send actionbar locale: missing namespace in '" + fullKey + "'");
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
            player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(text));
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "send actionbar locale " + key.toString(event, b) + " to " + players.toString(event, b);
    }
}
