package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.core.game.MiniGameManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class EffUnregisterGameMode extends Effect {
    private static final MiniGameManager MINI_GAME_MANAGER = MiniGameManager.getInstance();
    private Expression<MiniGame> gameMode;

    static {
        Skript.registerEffect(EffUnregisterGameMode.class,
                "unregister %gamemode%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        gameMode = (Expression<MiniGame>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        MiniGame[] miniGames = this.gameMode.getArray(event);
        for (MiniGame gm : miniGames) {
            if (gm == null) continue;
            if (MINI_GAME_MANAGER.isRegistered(gm.getId())) {
                MINI_GAME_MANAGER.unregisterMiniGame(gm.getId());
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "unregister gamemode " + gameMode.toString(event,b);
    }
}
