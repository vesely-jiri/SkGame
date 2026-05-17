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

@Name("MiniGame - Author")
@Description({
        "The author of a minigame.",
        "",
        "Supports: GET / SET / RESET."
})
@Examples({
        "set author of event-minigame to \"JuraJ_Player\"",
        "broadcast author of minigame with id \"bomberman\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprMiniGameAuthor extends SimplePropertyExpression<MiniGame, String> {

    static {
        register(ExprMiniGameAuthor.class, String.class, "minigame author", "minigame");
    }

    @Override
    public @Nullable String convert(MiniGame miniGame) {
        Object v = miniGame.getValue("author");
        return v != null ? v.toString() : null;
    }

    @Override
    public Class<? extends String> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> CollectionUtils.array(String.class);
            case RESET, DELETE -> CollectionUtils.array();
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        MiniGame mg = getExpr().getSingle(event);
        if (mg == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                mg.setValue("author", delta[0].toString());
            }
            case RESET, DELETE -> mg.removeValue("author");
        }
    }

    @Override
    protected String getPropertyName() {
        return "author";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }
}
