package cz.nox.skgame.skript.expressions.minigames.customValue.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.api.game.model.type.CustomValuePlurality;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class ExprCustomValuePlurality extends SimplePropertyExpression<CustomValue, CustomValuePlurality> {

    static {
        registerDefault(ExprCustomValuePlurality.class, CustomValuePlurality.class,
                "plurality", "customvalue");
    }

    @Override
    public @Nullable CustomValuePlurality convert(CustomValue from) {
        return from.getPlurality();
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET           -> CollectionUtils.array(CustomValuePlurality.class);
            case RESET, DELETE -> CollectionUtils.array();
            default            -> null;
        };
    }

    @Override
    public void change(Event e, Object @Nullable [] delta, Changer.ChangeMode mode) {
        CustomValue expr = getExpr().getSingle(e);
        if (expr == null || delta == null || delta[0] == null) {
            return;
        }
        CustomValuePlurality plur = (CustomValuePlurality) delta[0];
        switch (mode) {
            case SET:
                expr.setPlurality(plur);
                break;
            case RESET, DELETE:
                expr.setPlurality(null);
        }
    }

    @Override
    protected String getPropertyName() {
        return "plurality";
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends CustomValuePlurality> getReturnType() {
        return CustomValuePlurality.class;
    }
}
