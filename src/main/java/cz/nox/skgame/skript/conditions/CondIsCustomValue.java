package cz.nox.skgame.skript.conditions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Condition;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.CustomValue;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("CustomValue - Is Custom Value")
@Description({
        "Checks whether the given object is a CustomValue.",
        "",
        "Can also check if the object is a list of CustomValues.",
        "This is useful when working with dynamic values returned from expressions",
        "where the type is not guaranteed.",
        "",
        "Supports both single CustomValue and CustomValue list checks."
})
@Examples({
        "if {_value} is a custom value:",
        "    broadcast \"This is a valid CustomValue\"",
        "",
        "if {_values::*} is a custom list value:",
        "    broadcast \"This is a list of CustomValues\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class CondIsCustomValue extends Condition {

    private Expression<Object> obj;

    static {
        Skript.registerCondition(CondIsCustomValue.class,
                "%object% is [a] custom [[mini]game] value"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.obj = (Expression<Object>) exprs[0];
        return true;
    }

    @Override
    public boolean check(Event e) {
        Object o = this.obj.getSingle(e);
        if (o == null) return false;
        return CustomValue.class.isAssignableFrom(o.getClass());
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        return "is object a custom value";
    }
}
