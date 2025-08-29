package cz.nox.skgame.skript.expressions.minigames.property;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import cz.nox.skgame.api.game.model.MiniGame;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprMiniGameId extends SimplePropertyExpression<MiniGame, String> {

    static {
        register(ExprMiniGameId.class, String.class,
                "id","minigame");
    }

    @Override
    public @Nullable String convert(MiniGame miniGame) {
        return miniGame.getId();
    }

    @Override
    protected String getPropertyName() {
        return "id";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }
}
