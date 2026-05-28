package cz.nox.skgame.skript.expressions.minigames.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.MiniGame;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("MiniGame - Name")
@Description({
        "The display name of a minigame.",
        "",
        "Supports: GET / SET / RESET."
})
@Examples({
        "set name of event-minigame to \"&cBomberMan\"",
        "broadcast name of minigame with id \"bomberman\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprMiniGameName extends SimplePropertyExpression<MiniGame, String> {

    static {
        register(ExprMiniGameName.class, String.class, "minigame name", "minigame");
    }

    @Override
    public @Nullable String convert(MiniGame miniGame) {
        Object v = miniGame.getValue("name");
        return v != null ? v.toString() : null;
    }

    @Override
    public @Nullable Class<? extends String>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> CollectionUtils.array(String.class);
            case RESET, DELETE -> CollectionUtils.array();
            default -> null;
        };
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        MiniGame mg = getExpr().getSingle(event);
        if (mg == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                mg.setValue("name", delta[0].toString());
            }
            case RESET, DELETE -> mg.removeValue("name");
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
