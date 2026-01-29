package cz.nox.skgame.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class CondIsCustomValue extends Condition {

    private Expression<Object> obj;
    private boolean isList;

    static {
        Skript.registerCondition(CondIsCustomValue.class,
                "%object% is [a] custom [[mini]game] [list:list] value"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.obj = (Expression<Object>) exprs[0];
        this.isList = parseResult.hasTag("list");
        return true;
    }

    @Override
    public boolean check(Event e) {
        Object o = this.obj.getSingle(e);
        if (o == null) return false;
        if (isList) {
            return CustomValue[].class.isAssignableFrom(o.getClass());
        }
        return CustomValue.class.isAssignableFrom(o.getClass());
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        return "is object a custom value";
    }
}
