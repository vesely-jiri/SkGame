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

@Name("CustomValue - Description")
@Description({
        "Represents the textual description of a CustomValue.",
        "",
        "The description is typically used to explain the purpose or meaning",
        "of a CustomValue in a human-readable form.",
        "",
        "This property allows you to retrieve, change, or remove the description.",
        "",
        "Resetting or deleting this value clears the description (sets it to none).",
        "",
        "Supports: GET / SET / RESET / DELETE."
})
@Examples({
        "set {_cv} to custom value with id \"lives\"",
        "",
        "broadcast description of {_cv}",
        "",
        "set description of {_cv} to \"Number of lives a player has\"",
        "",
        "reset description of {_cv}"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprCustomValueDescription extends SimplePropertyExpression<CustomValue, String> {

    static {
        registerDefault(ExprCustomValueDescription.class, String.class,
                "[value] description", "customvalue");
    }

    @Override
    public @Nullable String convert(CustomValue from) {
        return from.getDescription();
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
        String description = (String) delta[0];
        switch (mode) {
            case SET -> v.setDescription(description);
            case RESET, DELETE -> v.setDescription(null);
        }
    }

    @Override
    protected String getPropertyName() {
        return "description";
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }
}
