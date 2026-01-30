package cz.nox.skgame.skript.expressions.minigames.customValue.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.CustomValue;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("CustomValue - Type")
@Description({
        "Represents the type of a CustomValue.",
        "",
        "The type defines what kind of value the CustomValue can hold,",
        "for example Number, String, Boolean, or any other supported type.",
        "",
        "This property allows you to retrieve, change, or clear the type of a CustomValue.",
        "",
        "Resetting or deleting this value removes the type (sets it to none).",
        "",
        "Supports: GET / SET / RESET / DELETE."
})
@Examples({
        "set {_cv} to custom value with id \"lives\"",
        "",
        "broadcast value type of {_cv}",
        "",
        "set value type of {_cv} to Number",
        "",
        "reset value type of {_cv}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprCustomValueType extends SimplePropertyExpression<CustomValue, Object> {

    static {
        registerDefault(ExprCustomValueType.class, Object.class,
                "value type", "customvalue");
    }

    @Override
    public @Nullable ClassInfo convert(CustomValue from) {
        return from.getType();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> CollectionUtils.array(Object.class);
            case RESET, DELETE -> CollectionUtils.array();
            default -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        CustomValue v = getExpr().getSingle(event);
        if (v == null || delta == null || delta[0] == null) return;
        ClassInfo<?> info = (ClassInfo<?>) delta[0];
        switch (mode) {
            case SET -> v.setType(info);
            case RESET, DELETE -> v.setType(null);
        }
    }

    @Override
    protected String getPropertyName() {
        return "value type";
    }

    @Override
    public Class<?> getReturnType() {
        return ClassInfo.class;
    }
}
