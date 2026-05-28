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

@Name("MiniGame - Description")
@Description({
        "The description of a minigame shown in the GUI.",
        "",
        "Supports: GET / SET / RESET."
})
@Examples({
        "set description of event-minigame to \"&7- Pickup your &cTNT &7and fight\"",
        "broadcast description of minigame with id \"bomberman\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprMiniGameDescription extends SimplePropertyExpression<MiniGame, String> {

    static {
        register(ExprMiniGameDescription.class, String.class, "minigame description", "minigame");
    }

    @Override
    public @Nullable String convert(MiniGame miniGame) {
        Object v = miniGame.getValue("description");
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
                mg.setValue("description", delta[0].toString());
            }
            case RESET, DELETE -> mg.removeValue("description");
        }
    }

    @Override
    protected String getPropertyName() {
        return "description";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }
}
