package cz.nox.skgame.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.util.Kleenean;
import cz.nox.skgame.core.game.GameMapManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public class EffSecRegisterGameMap extends EffectSection {

    private static final GameMapManager mapManager = GameMapManager.getInstance();
    private Expression<String> id;

    static {
        Skript.registerSection(EffSecRegisterGameMap.class,
                "(register|create) [new] [game] map (with|from) id %string%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult, @Nullable SectionNode sectionNode, @Nullable List<TriggerItem> list) {
        this.id = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable TriggerItem walk(Event event) {
        String id = this.id.getSingle(event);
        mapManager.createGameMap(id);
        return super.walk(event,false);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "create gamemap with id " + id.getSingle(event);
    }
}
