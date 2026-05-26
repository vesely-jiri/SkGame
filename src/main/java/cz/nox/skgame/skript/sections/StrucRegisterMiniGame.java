package cz.nox.skgame.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.registrations.EventValues;
import ch.njol.skript.variables.Variables;
import cz.nox.skgame.api.game.event.MiniGameRegisterEvent;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.core.game.MiniGameManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.DefaultSyntaxInfos.Structure.NodeType;

import java.util.List;

@Name("Register MiniGame")
@Description({
        "Registers a new minigame with the given ID at script load time.",
        "Can be used at the top level of a .sk file (no 'on load:' wrapper needed).",
        "The optional body executes immediately; use event-minigame to configure the game.",
        "Valid forms: register/create [new] minigame with/from id \"<id>\""
})
@Examples({
        "register minigame with id \"koth\":",
        "    set minigame name of event-minigame to \"King of the Hill\"",
        "    minigame tags: PVP, Team",
        "    set min players of event-minigame to 2",
        "",
        "create new minigame from id \"bomberman\":",
        "    set minigame name of event-minigame to \"&cBomberMan\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class StrucRegisterMiniGame extends Structure {

    private static final MiniGameManager miniGameManager = MiniGameManager.getInstance();

    private Literal<String> id;
    private @Nullable SectionNode sectionNode;
    private @Nullable List<TriggerItem> triggerItems;

    static {
        Skript.registerStructure(StrucRegisterMiniGame.class, null, NodeType.BOTH,
                "(register|create) [new] minigame (with|from) id %string%"
        );
        EventValues.registerEventValue(MiniGameRegisterEvent.class, MiniGame.class,
                MiniGameRegisterEvent::getMiniGame);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult,
                        @Nullable EntryContainer entryContainer) {
        this.id = (Literal<String>) args[0];
        if (entryContainer != null) {
            this.sectionNode = entryContainer.getSource();
        }
        return true;
    }

    @Override
    public boolean load() {
        if (sectionNode != null) {
            getParser().setCurrentEvent("minigame register", MiniGameRegisterEvent.class);
            try {
                triggerItems = ScriptLoader.loadItems(sectionNode);
            } finally {
                getParser().deleteCurrentEvent();
            }
        }
        return true;
    }

    @Override
    public boolean postLoad() {
        String minigameId = id.getSingle(null);
        if (minigameId == null) return false;
        MiniGame mg = miniGameManager.registerMiniGame(minigameId);
        if (triggerItems != null && !triggerItems.isEmpty()) {
            MiniGameRegisterEvent event = new MiniGameRegisterEvent(mg);
            TriggerItem.walk(triggerItems.get(0), event);
            Variables.removeLocals(event);
        }
        return true;
    }

    @Override
    public void unload() {
        String minigameId = id.getSingle(null);
        if (minigameId != null) {
            miniGameManager.unregisterMiniGame(minigameId);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "register minigame with id " + id.toString(event, debug);
    }
}
