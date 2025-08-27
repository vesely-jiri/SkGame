package cz.nox.skgame.skript.expressions.gamemaps.property;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import cz.nox.skgame.api.game.model.GameMap;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprGameMapId extends SimplePropertyExpression<GameMap, String> {

    static {
        register(ExprGameMapId.class, String.class,
                "id","gamemap");
    }

    @Override
    public @Nullable String convert(GameMap gameMap) {
        return gameMap.getId();
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
