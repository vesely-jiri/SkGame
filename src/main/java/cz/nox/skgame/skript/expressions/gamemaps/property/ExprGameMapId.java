package cz.nox.skgame.skript.expressions.gamemaps.property;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.GameMap;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("GameMap - ID")
@Description({"The unique ID of a GameMap.", "", "Supports: GET only."})
@Examples({"broadcast id of {_map}", "broadcast id of event-gamemap"})
@Since("1.0.0")
public class ExprGameMapId extends SimpleExpression<String> {
    private Expression<Object> expr;
    static { Skript.registerExpression(ExprGameMapId.class, String.class, ExpressionType.COMBINED,
            "[game]map id of %object%", "%object%'s [game]map id"); }
    @SuppressWarnings("unchecked") @Override
    public boolean init(Expression<?>[] e, int i, Kleenean k, SkriptParser.ParseResult r) { expr = (Expression<Object>) e[0]; return true; }
    @Override protected @Nullable String[] get(Event ev) {
        Object raw = expr.getSingle(ev); if (!(raw instanceof GameMap gm)) return null; return new String[]{gm.getId()}; }
    @Override public boolean isSingle() { return true; }
    @Override public Class<String> getReturnType() { return String.class; }
    @Override public String toString(@Nullable Event ev, boolean d) { return "id of " + expr.toString(ev, d); }
}
