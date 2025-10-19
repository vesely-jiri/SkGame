package cz.nox.skgame.skript.expressions.gamemaps;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.core.game.MiniGameManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;

@SuppressWarnings("unused")
public class ExprGameMapMiniGames extends SimpleExpression<Object> {
    private static final MiniGameManager miniGameManager = MiniGameManager.getInstance();
    private Expression<GameMap> map;

    static {
        Skript.registerExpression(ExprGameMapMiniGames.class, Object.class, ExpressionType.COMBINED,
                "[all] [supported] [mini]games of %gamemap%"
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.map = (Expression<GameMap>) exprs[0];
        return true;
    }

    @Nullable
    @Override
    protected Object[] get(Event event) {
        GameMap map = this.map.getSingle(event);
        if (map == null) return null;
        HashSet<MiniGame> mg = new HashSet<>();
        for (String id : map.getSupportedMiniGameIds()) {
            mg.add(miniGameManager.getMiniGameById(id));
        }
        return mg.toArray();
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<MiniGame> getReturnType() {
        return MiniGame.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean d) {
        return "minigames of " + this.map.toString(e,d);
    }
}
