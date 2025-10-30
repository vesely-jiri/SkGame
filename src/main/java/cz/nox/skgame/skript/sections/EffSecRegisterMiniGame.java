package cz.nox.skgame.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.*;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.event.MiniGameRegisterEvent;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.core.game.MiniGameManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Name("Register MiniGame")
@Description({
        "Registers a new MiniGame with a specified ID.",
        "If a section is provided, the code inside it will be executed immediately after the MiniGame is registered, with access to the 'event-minigame'.",
        "",
        "Useful for setting up game logic, rewards, or other MiniGame-specific properties upon registration.",
        "",
        "Supports: SECTION (code block).",
        "Supports: event-minigame"
})
@Examples({
        "register minigame with id \"bomberman\"",
        "register minigame with id \"bomberman\":",
        "\tset value \"author\" of event-minigame to player",
        "\tbroadcast \"MiniGame %event-minigame% registered!\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSecRegisterMiniGame extends EffectSection {

    private static final MiniGameManager miniGameManager = MiniGameManager.getInstance();
    private Expression<String> id;
    private Trigger trigger;

    static {
        Skript.registerSection(EffSecRegisterMiniGame.class,
                "(register|create) [new] minigame (with|from) id %string%"
        );
        EventValues.registerEventValue(MiniGameRegisterEvent.class, MiniGame.class,
              MiniGameRegisterEvent::getMiniGame);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult,
                        SectionNode sectionNode, List<TriggerItem> list) {
        if (hasSection()) {
            trigger = loadCode(sectionNode,"minigame register", MiniGameRegisterEvent.class);
        }

        this.id = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable TriggerItem walk(Event event) {
        Object localVars = Variables.copyLocalVariables(event);
        String id = this.id.getSingle(event);
        MiniGame mg = miniGameManager.registerMiniGame(id);

        if (hasSection()) {
            MiniGameRegisterEvent registerEvent = new MiniGameRegisterEvent(mg);
            Variables.setLocalVariables(registerEvent,localVars);
            TriggerItem.walk(trigger, registerEvent);
            Variables.setLocalVariables(event,Variables.copyLocalVariables(registerEvent));
            Variables.removeLocals(registerEvent);
        }

        return super.walk(event,false);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "register minigame with id " + id.getSingle(event);
    }
}
