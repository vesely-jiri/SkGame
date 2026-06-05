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
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("MiniGame - Author")
@Description({"The author of a minigame.", "", "Supports: GET / SET / RESET."})
@Examples({"broadcast author of event-minigame"})
@Since("1.0.0")
public class ExprMiniGameAuthor extends SimpleExpression<String> {
    private Expression<Object> expr;
    static { Skript.registerExpression(ExprMiniGameAuthor.class, String.class, ExpressionType.PROPERTY,
            "minigame author of %object%", "%object%'s minigame author"); }
    @SuppressWarnings("unchecked") @Override
    public boolean init(Expression<?>[] e, int i, Kleenean k, SkriptParser.ParseResult r) { expr = (Expression<Object>) e[0]; return true; }
    @Override protected @Nullable String[] get(Event ev) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof MiniGame mg)) return null;
        Object v = mg.getValue("author"); return v == null ? null : new String[]{v.toString()}; }
    @Override public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) { case SET -> CollectionUtils.array(String.class); case RESET, DELETE -> CollectionUtils.array(); default -> null; }; }
    @Override public void change(Event ev, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof MiniGame mg)) return;
        switch (mode) {
            case SET -> { if (delta == null || delta[0] == null) return; mg.setValue("author", delta[0].toString()); }
            case RESET, DELETE -> mg.removeValue("author"); } }
    @Override public boolean isSingle() { return true; }
    @Override public Class<String> getReturnType() { return String.class; }
    @Override public String toString(@Nullable Event ev, boolean d) { return "author of " + expr.toString(ev, d); }
}
