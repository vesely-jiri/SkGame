package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.GameMode;
import cz.nox.skgame.core.game.GameModeManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class EffUnregisterGameMode extends Effect {
    private static final GameModeManager gameModeManager = GameModeManager.getInstance();
    private Expression<GameMode> gameMode;

    static {
        Skript.registerEffect(EffUnregisterGameMode.class,
                "unregister %gamemode%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        gameMode = (Expression<GameMode>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        GameMode[] gameModes = this.gameMode.getArray(event);
        for (GameMode gm : gameModes) {
            if (gm == null) continue;
            if (gameModeManager.isRegistered(gm.getId())) {
                gameModeManager.unregisterGameMode(gm.getId());
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "unregister gamemode " + gameMode.toString(event,b);
    }
}
