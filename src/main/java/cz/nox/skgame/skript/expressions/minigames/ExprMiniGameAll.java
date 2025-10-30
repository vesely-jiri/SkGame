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
import cz.nox.skgame.core.game.MiniGameManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("MiniGame - All")
@Description({
        "Returns all registered MiniGames.",
        "",
        "Useful for iterating over or displaying all available MiniGames.",
        "",
        "Supports: GET only.",
})
@Examples({
        "loop all minigames:",
        "    broadcast \"MiniGame: %id of loop-minigame%\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprMiniGameAll extends SimpleExpression<MiniGame> {
    private static final MiniGameManager miniGameManager = MiniGameManager.getInstance();

    static {
        Skript.registerExpression(ExprMiniGameAll.class, MiniGame.class, ExpressionType.SIMPLE,
                "[all] minigames"
        );
    }

    @Override
    public boolean init(Expression<?>[] expressions, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    protected MiniGame @Nullable [] get(Event event) {
        return miniGameManager.getAllMiniGames();
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends MiniGame> getReturnType() {
        return MiniGame.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "all minigames";
    }
}
