package cz.nox.skgame.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class CondMapSupportsMiniGame extends Condition {
    private Expression<GameMap> map;
    private Expression<MiniGame> miniGame;

    static {
        Skript.registerCondition(CondMapSupportsMiniGame.class,
                "%gamemap% supports %minigame%"
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
        this.map = (Expression<GameMap>) exprs[0];
        this.miniGame = (Expression<MiniGame>) exprs[1];
        return true;
    }

    @Override
    public boolean check(Event e) {
        MiniGame mg = this.miniGame.getSingle(e);
        GameMap map = this.map.getSingle(e);
        if (mg == null || map == null) return false;
        return (map.supportsMiniGame(mg));
    }

    @Override
    public String toString(@Nullable Event e, boolean d) {
        return this.map.toString(e,d) + " supports " + this.miniGame.toString(e,d) ;
    }
}
