package cz.nox.skgame.skript.expressions.gamemaps.property;

import ch.njol.skript.expressions.base.SimplePropertyExpression;
import cz.nox.skgame.api.game.model.GameMap;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprGameMapName extends SimplePropertyExpression<GameMap, String> {

    static {
        register(ExprGameMapName.class, String.class,
                "name","gamemap");
    }

    @Override
    public @Nullable String convert(GameMap gameMap) {
        return gameMap.getName();
    }

    @Override
    protected String getPropertyName() {
        return "name";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }
}
