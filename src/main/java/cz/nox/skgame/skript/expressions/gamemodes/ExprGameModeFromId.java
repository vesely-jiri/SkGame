package cz.nox.skgame.skript.expressions.gamemodes;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.GameMode;
import cz.nox.skgame.core.game.GameModeManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@SuppressWarnings("unused")
public class ExprGameModeFromId extends SimpleExpression<GameMode> {
    private static final GameModeManager gameModeManager = GameModeManager.getInstance();
    private Expression<String> uuids;

    static {
        Skript.registerExpression(ExprGameModeFromId.class, GameMode.class, ExpressionType.COMBINED,
                "gamemode[s] from [[uu]id][s] %strings%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.uuids = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable GameMode[] get(Event event) {
        return Arrays.stream(this.uuids.getArray(event))
                .map(gameModeManager::getGameModeById)
                .toArray(GameMode[]::new);
    }

    @Override
    public boolean isSingle() {
        return this.uuids.isSingle();
    }

    @Override
    public Class<? extends GameMode> getReturnType() {
        return GameMode.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "gamemode[s] with id[s] " + this.uuids.toString(event,b);
    }
}
