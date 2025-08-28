package cz.nox.skgame.skript.expressions.minigames.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.MiniGame;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprMiniGameName extends SimplePropertyExpression<MiniGame, String> {

    static {
        register(ExprMiniGameName.class, String.class,
                "name","minigame");
    }

    @Override
    public @Nullable String convert(MiniGame miniGame) {
        return miniGame.getName();
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
        MiniGame miniGame = getExpr().getSingle(event);
        if (miniGame == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                String name = (String) delta[0];
                miniGame.setName(name);
            }
            case RESET -> miniGame.setName(null);
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
