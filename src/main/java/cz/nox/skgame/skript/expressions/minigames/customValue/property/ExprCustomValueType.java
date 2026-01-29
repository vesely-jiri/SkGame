package cz.nox.skgame.skript.expressions.minigames.customValue.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.CustomValue;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

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
