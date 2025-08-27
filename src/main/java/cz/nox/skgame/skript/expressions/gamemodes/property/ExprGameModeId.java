package cz.nox.skgame.skript.expressions.gamemodes.property;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import cz.nox.skgame.api.game.model.GameMode;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprGameModeId extends SimplePropertyExpression<GameMode, String> {

    static {
        register(ExprGameModeId.class, String.class,
                "id","gamemode");
    }

    @Override
    public @Nullable String convert(GameMode gameMode) {
        return gameMode.getId();
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
