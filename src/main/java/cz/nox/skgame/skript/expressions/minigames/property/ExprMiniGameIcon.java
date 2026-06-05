package cz.nox.skgame.skript.expressions.minigames.property;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.MiniGame;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("MiniGame - Icon")
@Description({"The icon (ItemStack) of a minigame.", "", "Supports: GET / SET / RESET."})
@Examples({"set icon of event-minigame to diamond sword"})
@Since("1.0.0")
public class ExprMiniGameIcon extends SimpleExpression<ItemStack> {
    private Expression<Object> expr;
    static { Skript.registerExpression(ExprMiniGameIcon.class, ItemStack.class, ExpressionType.PROPERTY,
            "minigame icon of %object%", "%object%'s minigame icon"); }
    @SuppressWarnings("unchecked") @Override
    public boolean init(Expression<?>[] e, int i, Kleenean k, SkriptParser.ParseResult r) { expr = (Expression<Object>) e[0]; return true; }
    @Override protected @Nullable ItemStack[] get(Event ev) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof MiniGame mg)) return null;
        Object v = mg.getValue("icon"); return v instanceof ItemStack is ? new ItemStack[]{is} : null; }
    @Override public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) { case SET -> CollectionUtils.array(ItemStack.class); case RESET, DELETE -> CollectionUtils.array(); default -> null; }; }
    @Override public void change(Event ev, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof MiniGame mg)) return;
        switch (mode) {
            case SET -> { if (delta == null || delta[0] == null) return; mg.setValue("icon", delta[0]); }
            case RESET, DELETE -> mg.removeValue("icon"); } }
    @Override public boolean isSingle() { return true; }
    @Override public Class<ItemStack> getReturnType() { return ItemStack.class; }
    @Override public String toString(@Nullable Event ev, boolean d) { return "icon of " + expr.toString(ev, d); }
}
