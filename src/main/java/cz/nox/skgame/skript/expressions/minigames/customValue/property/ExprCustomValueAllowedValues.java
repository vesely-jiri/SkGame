package cz.nox.skgame.skript.expressions.minigames.customValue.property;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
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
public class ExprCustomValueAllowedValues extends SimpleExpression<String> {

    private Expression<CustomValue> customValue;

    static {
        Skript.registerExpression(ExprCustomValueAllowedValues.class, String.class, ExpressionType.PROPERTY,
                "[the] allowed values of %customvalue%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.customValue = (Expression<CustomValue>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable String[] get(Event event) {
        CustomValue cv = this.customValue.getSingle(event);
        if (cv == null) return null;
        List<String> vals = cv.getAllowedValues();
        return vals.isEmpty() ? null : vals.toArray(new String[0]);
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
        CustomValue cv = this.customValue.getSingle(event);
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
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        return "allowed values of " + this.customValue.toString(e, b);
    }
}
