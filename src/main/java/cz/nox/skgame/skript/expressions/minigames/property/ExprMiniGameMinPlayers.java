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
@Name("MiniGame - Min Players")
@Description({"The minimum players required for a minigame.", "", "Supports: GET / SET / RESET."})
@Examples({"broadcast min players of event-minigame"})
@Since("1.0.0")
public class ExprMiniGameMinPlayers extends SimpleExpression<Number> {
    private Expression<Object> expr;
    static { Skript.registerExpression(ExprMiniGameMinPlayers.class, Number.class, ExpressionType.COMBINED,
            "min[imum] [minigame] players of %minigame%", "%minigame%'s min[imum] [minigame] players"); }
    @SuppressWarnings("unchecked") @Override
    public boolean init(Expression<?>[] e, int i, Kleenean k, SkriptParser.ParseResult r) { expr = (Expression<Object>) e[0]; return true; }
    @Override protected @Nullable Number[] get(Event ev) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof MiniGame mg)) return null;
        Object v = mg.getValue("min_players"); return v instanceof Number n ? new Number[]{n} : null; }
    @Override public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) { case SET -> CollectionUtils.array(Number.class); case RESET, DELETE -> CollectionUtils.array(); default -> null; }; }
    @Override public void change(Event ev, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof MiniGame mg)) return;
        switch (mode) {
            case SET -> { if (delta == null || delta[0] == null) return; mg.setValue("min_players", ((Number) delta[0]).longValue()); }
            case RESET, DELETE -> mg.removeValue("min_players"); } }
    @Override public boolean isSingle() { return true; }
    @Override public Class<Number> getReturnType() { return Number.class; }
    @Override public String toString(@Nullable Event ev, boolean d) { return "min players of " + expr.toString(ev, d); }
}
