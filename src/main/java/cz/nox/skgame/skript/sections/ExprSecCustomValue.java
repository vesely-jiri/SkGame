package cz.nox.skgame.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.expressions.base.SectionExpression;
import ch.njol.skript.lang.*;
import ch.njol.skript.lang.util.SectionUtils;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.CustomValue;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public class ExprSecCustomValue extends SectionExpression<CustomValue> {

    static {
        Skript.registerExpression(ExprSecCustomValue.class, CustomValue.class, ExpressionType.SIMPLE,
                "[a] custom [[mini]game] value");
        EventValues.registerEventValue(CreateCustomValueEvent.class, CustomValue.class, CreateCustomValueEvent::getCustomValue);
    }

    private Trigger trigger;

    @Override
    public boolean init(Expression<?>[] expressions, int pattern, Kleenean delayed, SkriptParser.ParseResult result, @Nullable SectionNode node, @Nullable List<TriggerItem> triggerItems) {
        if (node != null) {
            trigger = SectionUtils.loadLinkedCode("create custom value", (beforeLoading, afterLoading)
                        -> loadCode(node,"create custom value", beforeLoading, afterLoading, CreateCustomValueEvent.class));
            return trigger != null;
        }
        return false;
    }

    @Override
    protected CustomValue @Nullable [] get(Event e) {
        CustomValue customValue = new CustomValue();
        CreateCustomValueEvent createCustomValueEvent = new CreateCustomValueEvent(customValue);
        Variables.withLocalVariables(e, createCustomValueEvent, () -> TriggerItem.walk(trigger,createCustomValueEvent));
        return CollectionUtils.array(customValue);
    }

    @Override
    public boolean isSectionOnly() {
        return true;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<CustomValue> getReturnType() {
        return CustomValue.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "a custom value";
    }

    public static class CreateCustomValueEvent extends Event {
        private final CustomValue val;
        public CreateCustomValueEvent(CustomValue v) {
            this.val = v;
        }
        public CustomValue getCustomValue() {
            return val;
        }
        @Override
        public @NotNull HandlerList getHandlers() {
            throw new IllegalStateException();
        }
    }
}
