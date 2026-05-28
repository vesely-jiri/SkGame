package cz.nox.skgame.skript.expressions.gamemaps;

import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.region.Region;
import cz.nox.skgame.core.game.GameMapManager;
import cz.nox.skgame.core.region.RegionFactory;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("GameMap - Region")
@Description({
        "Gets or sets the main region of a GameMap.",
        "The region defines the play area for that map.",
        "",
        "Supports: GET / SET / DELETE."
})
@Examples({
        "set region of {_map} to {_region}",
        "set {_r} to region of {_map}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprGameMapRegion extends SimplePropertyExpression<GameMap, Region> {

    static {
        register(ExprGameMapRegion.class, Region.class, "region", "gamemap");
    }

    @Override
    public @Nullable Region convert(GameMap gameMap) {
        return gameMap.getRegion();
    }

    @Override
    public @Nullable Class<?>[] acceptChange(ChangeMode mode) {
        return switch (mode) {
            // Object.class is intentional: change() tries RegionFactory.adapt() for non-SkGame
            // region types (SkBee, WorldGuard) so users can pass native region objects directly.
            case SET -> CollectionUtils.array(Region.class, Object.class);
            case DELETE, RESET -> CollectionUtils.array();
            default -> null;
        };
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, ChangeMode mode) {
        GameMap gameMap = getExpr().getSingle(event);
        if (gameMap == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                Object val = delta[0];
                if (val instanceof Region r) {
                    gameMap.setRegion(r);
                } else {
                    Region adapted = RegionFactory.adapt(val);
                    if (adapted == null) return;
                    gameMap.setRegion(adapted);
                }
                GameMapManager.getInstance().save();
            }
            case DELETE, RESET -> {
                gameMap.setRegion(null);
                GameMapManager.getInstance().save();
            }
        }
    }

    @Override
    protected String getPropertyName() { return "region"; }

    @Override
    public Class<? extends Region> getReturnType() { return Region.class; }
}
