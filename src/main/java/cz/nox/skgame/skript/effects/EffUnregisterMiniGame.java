package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.core.game.MiniGameManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Unregister MiniGame")
@Description({
        "Unregisters a specific MiniGame from the MiniGameManager.",
        "",
        "Use this to remove a MiniGame from the server dynamically.",
        "",
        "Supports: EXECUTE only."
})
@Examples({
        "unregister {_minigame}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffUnregisterMiniGame extends Effect {
    private static final MiniGameManager miniGameManager = MiniGameManager.getInstance();
    private Expression<MiniGame> miniGame;

    static {
        Skript.registerEffect(EffUnregisterMiniGame.class,
                "unregister %minigame%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        miniGame = (Expression<MiniGame>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        MiniGame[] miniGames = this.miniGame.getArray(event);
        for (MiniGame gm : miniGames) {
            if (gm == null) continue;
            if (miniGameManager.isRegistered(gm.getId())) {
                miniGameManager.unregisterMiniGame(gm.getId());
            }
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "unregister minigame " + miniGame.toString(event,b);
    }
}
