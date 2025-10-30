package cz.nox.skgame.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("GameMap - Supports MiniGame")
@Description({
        "Checks if the specified game map supports the given mini-game.",
        "",
        "This is based on the MiniGame compatibility list of the GameMap object.",
        "Returns true if GameMap shares at least one value between the MiniGame"
})
@Examples({
        "if {_map} supports {_minigame}:",
        "\tbroadcast \"Map supports the MiniGame with id %{_minigame}%!\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class CondGameMapSupportsMiniGame extends Condition {
    private Expression<GameMap> map;
    private Expression<MiniGame> miniGame;

    static {
        Skript.registerCondition(CondGameMapSupportsMiniGame.class,
                "%gamemap% supports %minigame%",
                "%minigame% supports %gamemap%"
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
