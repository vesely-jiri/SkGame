package cz.nox.skgame.skript.expressions.minigames;

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
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.MinigameTag;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Minigame Tags")
@Description("Returns the tags of a minigame.")
@Examples({
        "set {_tags::*} to tags of event-minigame",
        "if tags of {_mg} contains \"pvp\":"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprMiniGameTags extends SimpleExpression<MinigameTag> {

    private Expression<MiniGame> minigameExpr;

    static {
        Skript.registerExpression(ExprMiniGameTags.class, MinigameTag.class, ExpressionType.COMBINED,
                "[the] tags of %minigame%",
                "%minigame%'[s] tags"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean kleenean,
                        SkriptParser.ParseResult parseResult) {
        minigameExpr = (Expression<MiniGame>) exprs[0];
        return true;
    }

    @Override
    protected MinigameTag @Nullable [] get(Event event) {
        MiniGame mg = minigameExpr.getSingle(event);
        if (mg == null) return new MinigameTag[0];
        return mg.getTags().toArray(new MinigameTag[0]);
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends MinigameTag> getReturnType() {
        return MinigameTag.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "tags of " + minigameExpr.toString(event, debug);
    }
}
