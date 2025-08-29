package cz.nox.skgame.skript.expressions.minigames;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.core.game.MiniGameManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@SuppressWarnings("unused")
public class ExprMiniGameFromId extends SimpleExpression<MiniGame> {
    private static final MiniGameManager miniGameManager = MiniGameManager.getInstance();
    private Expression<String> uuids;

    static {
        Skript.registerExpression(ExprMiniGameFromId.class, MiniGame.class, ExpressionType.COMBINED,
                "minigame[s] (with|from) [[uu]id][s] %strings%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.uuids = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable MiniGame[] get(Event event) {
        return Arrays.stream(this.uuids.getArray(event))
                .map(miniGameManager::getMiniGameById)
                .toArray(MiniGame[]::new);
    }

    @Override
    public boolean isSingle() {
        return this.uuids.isSingle();
    }

    @Override
    public Class<? extends MiniGame> getReturnType() {
        return MiniGame.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "minigame[s] with id[s] " + this.uuids.toString(event,b);
    }
}
