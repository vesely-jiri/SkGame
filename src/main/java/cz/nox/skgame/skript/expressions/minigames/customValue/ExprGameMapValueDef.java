package cz.nox.skgame.skript.expressions.minigames.customValue;

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
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.api.game.model.MiniGame;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("GameMap Value Definition")
@Description({
        "Returns the gamemap value schema entry (CustomValue) for a given key on a MiniGame.",
        "These are declared in the 'register minigame' section via 'set gamemap value ... of event-minigame to a custom value:'.",
        "Useful for inspecting the schema, e.g., reading allowed values or value type."
})
@Examples({
        "set {_def} to gamemap value def \"gamemode\" of (minigame with id \"bomberman\")",
        "send value name of {_def} to player"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprGameMapValueDef extends SimpleExpression<CustomValue> {

    static {
        Skript.registerExpression(ExprGameMapValueDef.class, CustomValue.class, ExpressionType.COMBINED,
                "[the] gamemap value def[inition] %string% of %minigame%"
        );
    }

    private Expression<String> key;
    private Expression<MiniGame> miniGame;

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, SkriptParser.ParseResult result) {
        key = (Expression<String>) expressions[0];
        miniGame = (Expression<MiniGame>) expressions[1];
        return true;
    }

    @Override
    protected @Nullable CustomValue[] get(Event event) {
        String k = key.getSingle(event);
        MiniGame mg = miniGame.getSingle(event);
        if (k == null || mg == null) return null;
        CustomValue cv = mg.getGameMapValueDef(k);
        return cv != null ? new CustomValue[]{cv} : null;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends CustomValue> getReturnType() {
        return CustomValue.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "gamemap value def " + key.toString(event, debug) + " of " + miniGame.toString(event, debug);
    }
}
