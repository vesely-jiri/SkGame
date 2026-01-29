package cz.nox.skgame.skript.expressions.minigames.customValue.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.CustomValue;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

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
