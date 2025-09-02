package cz.nox.skgame.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.event.GameMapRegisterEvent;
import cz.nox.skgame.api.game.event.MiniGameRegisterEvent;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.core.game.GameMapManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public class EffSecRegisterGameMap extends EffectSection {

    private static final GameMapManager mapManager = GameMapManager.getInstance();
    private Expression<String> id;
    private Trigger trigger;

    static {
        Skript.registerSection(EffSecRegisterGameMap.class,
                "(register|create) [new] [game] map (with|from) id %string%"
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
    public String toString(@Nullable Event event, boolean b) {
        return "create gamemap with id " + id.getSingle(event);
    }
}
