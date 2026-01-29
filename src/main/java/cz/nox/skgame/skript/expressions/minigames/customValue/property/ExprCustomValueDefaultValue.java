package cz.nox.skgame.skript.expressions.minigames.customValue.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.CustomValue;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprCustomValueDefaultValue extends SimplePropertyExpression<CustomValue, Object> {

    static {
        registerDefault(ExprCustomValueDefaultValue.class, Object.class,
                "default [value]", "customvalue");
    }

    @Override
    public @Nullable Object convert(CustomValue from) {
        return from.getDefaultValue();
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
        Object info = delta[0];
        switch (mode) {
            case SET -> v.setDefaultValue(info);
            case RESET, DELETE -> v.setDefaultValue(null);
        }
    }

    @Override
    protected String getPropertyName() {
        return "default value of custom value";
    }

    @Override
    public Class<?> getReturnType() {
        return Object.class;
    }
}
