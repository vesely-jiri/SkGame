package cz.nox.skgame.skript.expressions.gamemodes;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.GameMode;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprGameModeValue extends SimplePropertyExpression<GameMode, String> {

    static {
        register(ExprGameModeValue.class, String.class,
                "name","gamemode");
    }

    @Override
    public @Nullable String convert(GameMode gameMode) {
        return gameMode.getName();
    }

    @Override
    public Class<? extends String> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET   -> CollectionUtils.array(String.class);
            case RESET -> CollectionUtils.array();
            default    -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        GameMode gameMode = getExpr().getSingle(event);
        if (gameMode == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                String name = (String) delta[0];
                gameMode.setName(name);
            }
            case RESET -> gameMode.setName(null);
        }
    }

    @Override
    protected String getPropertyName() {
        return "name";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }
}
