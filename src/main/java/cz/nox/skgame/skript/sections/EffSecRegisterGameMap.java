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
import cz.nox.skgame.api.game.event.GameMapRegisterEvent;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.core.game.GameMapManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Name("Register GameMap")
@Description({
        "Registers a new game map with a specified ID.",
        "If a section is provided, the code inside it will be executed immediately after the map is registered, with access to the 'event-gamemap'.",
        "",
        "Useful for initializing spawn points, settings, or other map-specific properties.",
        "",
        "Supports: SECTION (code block).",
        "Supports: event-gamemap"
})
@Examples({
        "register gamemap with id \"arena_battle\"",
        "register gamemap with id \"arena_battle\":",
        "\tbroadcast \"GameMap %event-gamemap% registered!\"",
        "\tset value \"author\" of event-gamemap to player"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSecRegisterGameMap extends EffectSection {

    private static final GameMapManager mapManager = GameMapManager.getInstance();
    private Expression<String> id;
    private Trigger trigger;

    static {
        Skript.registerSection(EffSecRegisterGameMap.class,
                "(register|create) [new] [game[ ]]map (with|from) id %string%"
        );
        EventValues.registerEventValue(GameMapRegisterEvent.class, GameMap.class,
                GameMapRegisterEvent::getGameMap);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult, SectionNode sectionNode, @Nullable List<TriggerItem> list) {
        if (hasSection()) {
            this.trigger = loadCode(sectionNode, "register gamemap", GameMapRegisterEvent.class);
        }
        this.id = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable TriggerItem walk(Event e) {
        Object localVars = Variables.copyLocalVariables(e);
        String id = this.id.getSingle(e);
        GameMap gameMap = mapManager.registerGameMap(id);

        if (hasSection()) {
            GameMapRegisterEvent registerEvent = new GameMapRegisterEvent(gameMap);
            Variables.setLocalVariables(registerEvent,localVars);
            TriggerItem.walk(this.trigger,registerEvent);
            Variables.setLocalVariables(e, Variables.copyLocalVariables(registerEvent));
            Variables.removeLocals(registerEvent);
        }

        return super.walk(e,false);
    }

    @Override
    public String toString(@Nullable Event e, boolean b) {
        return "create gamemap with id " + this.id.toString(e,b);
    }
}
