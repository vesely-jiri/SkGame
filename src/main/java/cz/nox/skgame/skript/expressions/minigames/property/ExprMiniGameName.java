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
@Name("MiniGame - Name")
@Description({"The display name of a minigame.", "", "Supports: GET / SET / RESET."})
@Examples({"set name of event-minigame to \"&cBomberMan\""})
@Since("1.0.0")
public class ExprMiniGameName extends SimpleExpression<String> {
    private Expression<Object> expr;
    static { Skript.registerExpression(ExprMiniGameName.class, String.class, ExpressionType.COMBINED,
            "minigame name of %minigame%", "%minigame%'s minigame name"); }
    @SuppressWarnings("unchecked") @Override
    public boolean init(Expression<?>[] e, int i, Kleenean k, SkriptParser.ParseResult r) { expr = (Expression<Object>) e[0]; return true; }
    @Override protected @Nullable String[] get(Event ev) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof MiniGame mg)) return null;
        Object v = mg.getValue("name"); return v == null ? null : new String[]{v.toString()}; }
    @Override public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) { case SET -> CollectionUtils.array(String.class); case RESET, DELETE -> CollectionUtils.array(); default -> null; }; }
    @Override public void change(Event ev, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof MiniGame mg)) return;
        switch (mode) {
            case SET -> { if (delta == null || delta[0] == null) return; mg.setValue("name", delta[0].toString()); }
            case RESET, DELETE -> mg.removeValue("name"); } }
    @Override public boolean isSingle() { return true; }
    @Override public Class<String> getReturnType() { return String.class; }
    @Override public String toString(@Nullable Event ev, boolean d) { return "name of " + expr.toString(ev, d); }
}
