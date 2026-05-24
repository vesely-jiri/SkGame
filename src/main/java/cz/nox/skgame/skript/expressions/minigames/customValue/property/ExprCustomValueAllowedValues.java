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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unused")
@Name("CustomValue - Allowed Values")
@Description({
        "The list of allowed string values for a CustomValue.",
        "When non-empty, the admin GUI shows an enum picker instead of a free-text input.",
        "",
        "Supports: GET / SET / ADD / REMOVE / RESET."
})
@Examples({
        "set gamemap value \"gamemode\" of event-minigame to a custom value:",
        "    set value type to a text",
        "    set allowed values to \"elimination\", \"deathmatch\""
})
@Since("1.0.0")
public class ExprCustomValueAllowedValues extends SimplePropertyExpression<CustomValue, String> {

    static {
        registerDefault(ExprCustomValueAllowedValues.class, String.class,
                "allowed values", "customvalue");
    }

    @Override
    protected String[] get(Event event, CustomValue[] source) {
        List<String> result = new ArrayList<>();
        for (CustomValue cv : source) {
            result.addAll(cv.getAllowedValues());
        }
        return result.toArray(new String[0]);
    }

    @Nullable
    @Override
    public String convert(CustomValue from) {
        return null; // superseded by get(Event, CustomValue[]) override
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET, ADD, REMOVE -> CollectionUtils.array(String[].class);
            case RESET, DELETE    -> CollectionUtils.array();
            default               -> null;
        };
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, Changer.ChangeMode mode) {
        CustomValue cv = getExpr().getSingle(event);
        if (cv == null) return;
        String[] values = (delta == null) ? new String[0] :
                Arrays.copyOf(delta, delta.length, String[].class);
        switch (mode) {
            case SET -> cv.setAllowedValues(Arrays.asList(values));
            case ADD -> {
                List<String> list = new ArrayList<>(cv.getAllowedValues());
                list.addAll(Arrays.asList(values));
                cv.setAllowedValues(list);
            }
            case REMOVE -> {
                List<String> list = new ArrayList<>(cv.getAllowedValues());
                list.removeAll(Arrays.asList(values));
                cv.setAllowedValues(list);
            }
            case RESET, DELETE -> cv.setAllowedValues(new ArrayList<>());
        }
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    protected String getPropertyName() {
        return "allowed values";
    }
}
