package cz.nox.skgame.skript.expressions.minigames.property;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import cz.nox.skgame.api.game.model.MiniGame;
import org.jetbrains.annotations.Nullable;

@Name("MiniGame - ID")
@Description({
        "Represents the unique ID of a MiniGame.",
        "You can retrieve this value to identify or reference the MiniGame.",
        "",
        "Supports: GET only."
})
@Examples({
        "set {_minigame} to minigame with id \"bomberman\"",
        "broadcast id of {_minigame}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprMiniGameId extends SimplePropertyExpression<MiniGame, String> {

    static {
        register(ExprMiniGameId.class, String.class,
                "id","minigame");
    }

    @Override
    public @Nullable String convert(MiniGame miniGame) {
        return miniGame.getId();
    }

    @Override
    protected String getPropertyName() {
        return "id";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }
}
