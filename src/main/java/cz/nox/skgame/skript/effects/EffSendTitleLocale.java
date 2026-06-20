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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Send Title Locale")
@Description({
        "Sends a localized title to one or more players using their own locale.",
        "The key format is \"namespace:message.key\".",
        "The resolved text is shown as the main title. Subtitle is empty.",
})
@Examples({
        "# Title to a single player",
        "send title locale \"koth:game.start\" to event-player",
        "",
        "# Title to all session players",
        "send title locale \"bomberman:round.start\" to session players of event-session",
        "",
        "# Combine with regular Skript title for subtitle",
        "send title locale \"koth:game.over\" to event-player"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSendTitleLocale extends Effect {

    private Expression<String> key;
    private Expression<Player> players;
    @Nullable private Expression<Object> args;

    static {
        Skript.registerEffect(EffSendTitleLocale.class,
                "send title locale %string% to %players% [with [arguments] %-objects%]"
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
            SkGame.getInstance().getLogger().warning("[SkGame/ScriptLocale] send title locale: missing namespace in '" + fullKey + "'");
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
            Component titleComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(text);
            player.showTitle(Title.title(titleComponent, Component.empty()));
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "send title locale " + key.toString(event, b) + " to " + players.toString(event, b);
    }
}
