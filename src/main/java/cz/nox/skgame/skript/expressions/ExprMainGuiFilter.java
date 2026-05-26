package cz.nox.skgame.skript.expressions;

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
import cz.nox.skgame.core.gui.services.MainGuiService;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Main GUI Session Filter")
@Description({
        "Gets or sets the session filter string applied to the main GUI for a player.",
        "Setting to empty string or deleting clears the filter.",
        "Supports: GET, SET, DELETE, RESET."
})
@Examples({
        "set main gui filter of player to \"koth\"",
        "delete main gui filter of player",
        "set {_f} to main gui filter of player"
})
@Since("Phase 12")
public class ExprMainGuiFilter extends SimpleExpression<String> {

    private Expression<Player> player;

    static {
        Skript.registerExpression(ExprMainGuiFilter.class, String.class, ExpressionType.PROPERTY,
                "[the] [main[ ]gui] [session] filter of %player%",
                "%player%'s [main[ ]gui] [session] filter");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        player = (Expression<Player>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable String[] get(Event event) {
        Player p = player.getSingle(event);
        if (p == null) return new String[0];
        String filter = MainGuiService.getInstance().getFilter(p);
        return filter != null ? new String[]{filter} : new String[0];
    }

    @Override
    public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> new Class[]{String.class};
            case DELETE, RESET -> new Class[0];
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        Player p = player.getSingle(event);
        if (p == null) return;
        switch (mode) {
            case SET -> {
                String val = delta != null && delta.length > 0 ? (String) delta[0] : null;
                MainGuiService.getInstance().setFilter(p, val);
            }
            case DELETE, RESET -> MainGuiService.getInstance().setFilter(p, null);
        }
    }

    @Override
    public boolean isSingle() { return true; }

    @Override
    public Class<? extends String> getReturnType() { return String.class; }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "main gui filter of " + player.toString(event, debug);
    }
}
