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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name("MiniGame - Icon")
@Description({
        "The GUI icon of a minigame. Accepts any item.",
        "",
        "Supports: GET / SET / RESET."
})
@Examples({
        "set icon of event-minigame to TNT",
        "set icon of event-minigame to diamond sword named \"&bBattle Sword\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprMiniGameIcon extends SimplePropertyExpression<MiniGame, ItemStack> {

    static {
        register(ExprMiniGameIcon.class, ItemStack.class, "minigame icon", "minigame");
    }

    @Override
    public @Nullable ItemStack convert(MiniGame miniGame) {
        Object v = miniGame.getValue("icon");
        if (v instanceof ItemStack stack) return stack;
        return null;
    }

    @Override
    public Class<? extends ItemStack> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> CollectionUtils.array(ItemStack.class);
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
                mg.setValue("icon", delta[0]);
            }
            case RESET, DELETE -> mg.removeValue("icon");
        }
    }

    @Override
    protected String getPropertyName() {
        return "icon";
    }

    @Override
    public Class<? extends ItemStack> getReturnType() {
        return ItemStack.class;
    }
}
