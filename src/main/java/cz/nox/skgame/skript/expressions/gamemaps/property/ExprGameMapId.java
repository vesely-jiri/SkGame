package cz.nox.skgame.skript.expressions.gamemaps.property;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import cz.nox.skgame.api.game.model.GameMap;
import org.jetbrains.annotations.Nullable;

@Name("GameMap - ID")
@Description({
        "Represents the unique identifier (ID) of a GameMap.",
        "",
        "Use this expression to get the ID of a GameMap for reference, logging, or comparison.",
        "",
        "Supports: GET only."
})
@Examples({
        "set {_map} to gamemap with id \"arena_battle\"",
        "broadcast id of {_map}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprGameMapId extends SimplePropertyExpression<GameMap, String> {

    static {
        register(ExprGameMapId.class, String.class,
                "[game]map id","gamemap");
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
