package cz.nox.skgame.skript.expressions.minigames.customValue.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.api.game.model.type.CustomValuePlurality;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("CustomValue - Plurality")
@Description({
        "Represents the plurality setting of a CustomValue.",
        "",
        "Plurality defines how the CustomValue should be grammatically treated",
        "when used in messages or text output (for example singular or plural forms).",
        "",
        "This property allows you to retrieve, change, or clear the plurality of a CustomValue.",
        "",
        "Resetting or deleting this value removes the plurality setting (sets it to none).",
        "",
        "Supports: GET / SET / RESET / DELETE."
})
@Examples({
        "set {_cv} to custom value with id \"lives\"",
        "",
        "broadcast plurality of {_cv}",
        "",
        "set plurality of {_cv} to singular",
        "",
        "reset plurality of {_cv}"
})
@Since("1.0.0")
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
