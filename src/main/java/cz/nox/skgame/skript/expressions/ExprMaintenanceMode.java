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
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.SkGame;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Server Maintenance Mode")
@Description({
        "Returns or sets the server maintenance mode flag.",
        "When maintenance is on, no new games can start and multi-round auto-restart is skipped.",
        "Running games finish their current round before stopping.",
        "",
        "Supports: GET, SET."
})
@Examples({
        "if maintenance mode:",
        "    send \"Server is in maintenance\" to player",
        "set maintenance mode to true",
        "set maintenance mode to false"
})
@Since("1.0.0")
public class ExprMaintenanceMode extends SimpleExpression<Boolean> {

    static {
        Skript.registerExpression(ExprMaintenanceMode.class, Boolean.class, ExpressionType.SIMPLE,
                "[server] maintenance [mode]"
        );
    }

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                        SkriptParser.ParseResult parseResult) {
        return true;
    }

    @Override
    protected @Nullable Boolean[] get(Event event) {
        return new Boolean[]{ SkGame.getInstance().isMaintenanceMode() };
    }

    @Override
    public @Nullable Class<? extends Boolean>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> CollectionUtils.array(Boolean.class);
            default  -> null;
        };
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        if (delta == null || delta[0] == null) return;
        SkGame.getInstance().setMaintenanceMode((Boolean) delta[0]);
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends Boolean> getReturnType() {
        return Boolean.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "server maintenance mode";
    }
}
