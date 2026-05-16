package cz.nox.skgame.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.EffectSection;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.core.game.MiniGameManager;
import cz.nox.skgame.skript.sections.ExprSecCustomValue.CreateCustomValueEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Name("Section - Set GameMap Value Definition")
@Description({
        "Defines a gamemap value schema entry on a MiniGame.",
        "The section body uses 'event-value' (same as the custom value section) to configure the definition.",
        "Gamemap values are per-map config data keyed per minigame, separate from per-game runtime values.",
        "",
        "Supports: SECTION."
})
@Examples({
        "set gamemap value \"spawn_radius\" of event-minigame to a custom value:",
        "    set name of event-value to \"Spawn radius\"",
        "    set value type of event-value to a number",
        "    set default value of event-value to 5",
        "    set description of event-value to \"Radius around spawn\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSecSetGameMapValueDef extends EffectSection {

    private Expression<String> key;
    private Expression<MiniGame> miniGame;
    private Trigger trigger;

    static {
        Skript.registerSection(EffSecSetGameMapValueDef.class,
                "set gamemap value %string% of %minigame% to [a] custom value"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult,
                        SectionNode sectionNode, List<TriggerItem> list) {
        this.key = (Expression<String>) exprs[0];
        this.miniGame = (Expression<MiniGame>) exprs[1];
        if (hasSection()) {
            trigger = loadCode(sectionNode, "gamemap value def", CreateCustomValueEvent.class);
        }
        return true;
    }

    @Override
    protected @Nullable TriggerItem walk(Event event) {
        String k = key.getSingle(event);
        MiniGame mg = miniGame.getSingle(event);
        if (k == null || mg == null) return super.walk(event, false);

        CustomValue cv = new CustomValue();

        if (hasSection() && trigger != null) {
            Object localVars = Variables.copyLocalVariables(event);
            CreateCustomValueEvent cvEvent = new CreateCustomValueEvent(cv);
            Variables.setLocalVariables(cvEvent, localVars);
            TriggerItem.walk(trigger, cvEvent);
            Variables.setLocalVariables(event, Variables.copyLocalVariables(cvEvent));
            Variables.removeLocals(cvEvent);
        }

        mg.setGameMapValueDef(k, cv);
        MiniGameManager.getInstance().save();

        return super.walk(event, false);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "set gamemap value " + key.toString(event, debug) + " of " + miniGame.toString(event, debug);
    }
}
