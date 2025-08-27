package cz.nox.skgame.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.lang.*;
import ch.njol.util.Kleenean;
import cz.nox.skgame.core.game.GameModeManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("unused")
public class EffSecRegisterGameMode extends EffectSection {

    private static final GameModeManager gameModeManager = GameModeManager.getInstance();
    private Expression<String> id;

    static {
        Skript.registerSection(EffSecRegisterGameMode.class,
                "(register|create) [new] game[mode] (with|from) id %string%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult, SectionNode sectionNode, List<TriggerItem> list) {
        this.id = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable TriggerItem walk(Event event) {
        String id = this.id.getSingle(event);

        // TODO - Check if script file contains required events

        gameModeManager.registerGameMode(id);
        return super.walk(event,false);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "register gamemode with id " + id.getSingle(event);
    }
}
