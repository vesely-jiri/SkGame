package cz.nox.skgame.skript.expressions.gamemaps;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import cz.nox.skgame.api.game.model.GameMap;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("GameMap - Arena Slot Count")
@Description("Returns the number of configured arena slots for a GameMap.")
@Examples("set {_n} to arena slot count of {_map}")
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprGameMapArenaSlotCount extends SimplePropertyExpression<GameMap, Number> {

    static {
        register(ExprGameMapArenaSlotCount.class, Number.class, "arena slot count", "gamemap");
    }

    @Override
    public @Nullable Number convert(GameMap gameMap) {
        return gameMap.getConfiguredSlotCount();
    }

    @Override
    protected String getPropertyName() { return "arena slot count"; }

    @Override
    public Class<? extends Number> getReturnType() { return Number.class; }
}
