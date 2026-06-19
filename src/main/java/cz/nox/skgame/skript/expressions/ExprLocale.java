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
import cz.nox.skgame.SkGame;
import cz.nox.skgame.core.locale.ScriptLocaleRegistry;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Locale String")
@Description({
        "Returns the localized string for a specific player from a script-defined locale namespace.",
        "Key format: \"namespace:message.key\".",
        "Returns the raw key as fallback if the key is not found (and logs a warning).",
})
@Examples({
        "set {_text} to locale \"bomberman:game.start\" for player",
        "send title (locale \"bomberman:round.start\" for player) to player",
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprLocale extends SimpleExpression<String> {

    private Expression<String> key;
    private Expression<Player> player;

    static {
        Skript.registerExpression(ExprLocale.class, String.class, ExpressionType.COMBINED,
                "locale %string% for %player%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.key = (Expression<String>) exprs[0];
        this.player = (Expression<Player>) exprs[1];
        return true;
    }

    @Override
    protected @Nullable String[] get(Event event) {
        String fullKey = key.getSingle(event);
        Player p = player.getSingle(event);
        if (fullKey == null) return null;

        int colon = fullKey.indexOf(':');
        if (colon < 0) {
            SkGame.getInstance().getLogger().warning("[SkGame/ScriptLocale] locale expression: missing namespace in '" + fullKey + "'");
            return new String[]{fullKey};
        }
        String namespace = fullKey.substring(0, colon);
        String messageKey = fullKey.substring(colon + 1);

        String text = ScriptLocaleRegistry.getInstance().get(namespace, messageKey, p);
        if (text == null) {
            SkGame.getInstance().getLogger().warning("[SkGame/ScriptLocale] missing key: " + fullKey);
            return new String[]{fullKey};
        }
        return new String[]{text};
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
        return "locale " + key.toString(event, debug) + " for " + player.toString(event, debug);
    }
}
