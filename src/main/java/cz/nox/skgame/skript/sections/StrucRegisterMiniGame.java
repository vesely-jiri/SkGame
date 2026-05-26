package cz.nox.skgame.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.ScriptLoader;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.registrations.EventValues;
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
        "Use at the top level of a .sk file — no 'on load:' wrapper needed.",
        "The optional body runs immediately with event-minigame available.",
        "All shorthand effects (name:, author:, min players:, minigame tags:, ...) work inside the body.",
        "Valid forms: register/create [new] minigame with/from id \"<id>\""
})
@Examples({
        "register minigame with id \"koth\":",
        "    name: \"King of the Hill\"",
        "    author: \"JuraJ_Player\"",
        "    min players: 2",
        "    minigame tags: PVP, Team",
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
    private @Nullable Trigger trigger;

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
        if (sectionNode == null) return true;

        ParserInstance parser = getParser();
        // Replicate Section.loadCode(): backup → reset → set event context → loadItems → restore
        ParserInstance.Backup backup = parser.backup();
        parser.reset();
        parser.setCurrentEvent("minigame register", MiniGameRegisterEvent.class);

        SkriptEvent placeholder = new SkriptEvent() {
            @Override
            public boolean init(Literal<?>[] args, int matchedPattern, ParseResult parseResult) {
                return false;
            }
            @Override
            public boolean check(Event event) {
                return false;
            }
            @Override
            public String toString(@Nullable Event event, boolean debug) {
                return "minigame register";
            }
        };
        parser.setCurrentStructure(placeholder);

        List<TriggerItem> items = ScriptLoader.loadItems(sectionNode);
        trigger = new Trigger(parser.getCurrentScript(), "minigame register", placeholder, items);

        parser.restoreBackup(backup);
        return true;
    }

    @Override
    public boolean postLoad() {
        String minigameId = id.getSingle(null);
        if (minigameId == null) return false;
        MiniGame mg = miniGameManager.registerMiniGame(minigameId);
        if (trigger != null) {
            MiniGameRegisterEvent event = new MiniGameRegisterEvent(mg);
            trigger.execute(event);
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
