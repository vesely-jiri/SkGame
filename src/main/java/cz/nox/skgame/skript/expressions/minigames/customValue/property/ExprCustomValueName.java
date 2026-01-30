package cz.nox.skgame.skript.expressions.minigames.customValue.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.CustomValue;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("CustomValue - Name")
@Description({
        "Represents the name of a CustomValue.",
        "",
        "The name is used as a human-readable identifier for the CustomValue.",
        "It can be displayed to users or used for descriptive purposes.",
        "",
        "This property allows you to retrieve, change, or clear the name of a CustomValue.",
        "",
        "Resetting or deleting this value clears the name (sets it to none).",
        "",
        "Supports: GET / SET / RESET / DELETE."
})
@Examples({
        "set {_cv} to custom value with id \"lives\"",
        "",
        "broadcast value name of {_cv}",
        "",
        "set value name of {_cv} to \"Player Lives\"",
        "",
        "reset value name of {_cv}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprCustomValueName extends SimplePropertyExpression<CustomValue, String> {

    static {
        registerDefault(ExprCustomValueName.class, String.class,
                "value name", "customvalue");
    }

    @Override
    public @Nullable String convert(CustomValue from) {
        return from.getName();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> CollectionUtils.array(String.class);
            case RESET, DELETE -> CollectionUtils.array();
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        CustomValue v = getExpr().getSingle(event);
        if (v == null || delta == null || delta[0] == null) return;
        String name = (String) delta[0];
        switch (mode) {
            case SET -> v.setName(name);
            case RESET, DELETE -> v.setName(null);
        }
    }

    @Override
    protected String getPropertyName() {
        return "name";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }
}
