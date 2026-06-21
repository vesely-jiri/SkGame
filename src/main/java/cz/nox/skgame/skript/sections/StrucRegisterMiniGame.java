package cz.nox.skgame.skript.sections;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.Trigger;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.parser.ParserInstance;
import ch.njol.skript.lang.util.SimpleEvent;
import ch.njol.skript.registrations.EventValues;
import cz.nox.skgame.api.game.event.MiniGameRegisterEvent;
import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.MinigameTag;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.TeamEntry;
import cz.nox.skgame.api.game.model.TeamRules;
import cz.nox.skgame.api.game.model.type.CancellableEventType;
import cz.nox.skgame.api.game.model.type.DisbandReason;
import cz.nox.skgame.api.game.model.type.TeamAssignmentMode;
import cz.nox.skgame.core.game.MiniGameManager;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;
import org.skriptlang.skript.registration.DefaultSyntaxInfos.Structure.NodeType;

import java.util.ArrayList;
import java.util.List;

@Name("Register MiniGame")
@Description({
        "Registers a new minigame with the given ID at script load time.",
        "Use at the top level of a .sk file — no 'on load:' wrapper needed.",
        "The optional body accepts entry shorthand: name, icon, description, author, min players, minigame tags.",
        "Valid forms: register/create [new] minigame with/from id \"<id>\""
})
@Examples({
        "# Minimal — id only",
        "register minigame with id \"koth\"",
        "",
        "# Standard registration",
        "register minigame with id \"koth\":",
        "    name: \"King of the Hill\"",
        "    description: \"Hold the hill to score points\"",
        "    author: \"JuraJ_Player\"",
        "    icon: iron sword",
        "    min players: 2",
        "    minigame tags: pvp, team",
        "",
        "# Full registration — all supported entries",
        "register minigame with id \"bomberman\":",
        "    name: \"&cBomberMan\"",
        "    description: \"Place bombs to eliminate opponents\"",
        "    author: \"JuraJ_Player\"",
        "    icon: tnt",
        "    min players: 2",
        "    minigame tags: pvp",
        "    cancel events: fall-damage, hunger, item-drop",
        "    time: 18000",
        "    weather: \"storm\"",
        "    team assignment: auto",
        "    teams:",
        "        red:",
        "            name: \"&cRed Team\"",
        "            icon: red wool",
        "        blue:",
        "            name: \"&9Blue Team\"",
        "            icon: blue wool",
        "    values:",
        "        gamemap value \"spawn_points\":",
        "            name: \"Spawn points\"",
        "            type: a location",
        "            plurality: plural",
        "        session value \"kills\":",
        "            name: \"Kill count\"",
        "            type: a number",
        "            default: 0",
        "",
        "# With callback block — effects run once at script load time",
        "create new minigame from id \"spleef\":",
        "    name: \"Spleef\"",
        "    broadcast \"Minigame %event-minigame% registered!\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class StrucRegisterMiniGame extends Structure {

    private static final MiniGameManager miniGameManager = MiniGameManager.getInstance();

    private Literal<String> id;
    private @Nullable Trigger trigger;

    private @Nullable Expression<String>             nameExpr;
    private @Nullable Expression<ItemStack>          iconExpr;
    private @Nullable Expression<String>             descriptionExpr;
    private @Nullable Expression<String>             authorExpr;
    private @Nullable Expression<Number>             minPlayersExpr;
    private @Nullable Expression<Number>             maxPlayersExpr;
    private @Nullable Expression<MinigameTag>        tagsExpr;
    private @Nullable List<TeamEntry>                parsedTeams;
    private @Nullable TeamRules                                   defaultTeamRules;
    private @Nullable Expression<TeamAssignmentMode>             teamAssignmentExpr;
    private @Nullable List<MiniGameEntryHelper.ValueDefEntry>    parsedValues;
    private @Nullable Expression<CancellableEventType>           cancelEventsExpr;
    private @Nullable Expression<Number>                         timeExpr;
    private @Nullable Expression<String>                         weatherExpr;
    private @Nullable Expression<String>                         instructionsExpr;

    static {
        Skript.registerStructure(StrucRegisterMiniGame.class, MiniGameEntryHelper.MIXED, NodeType.BOTH,
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
            nameExpr        = MiniGameEntryHelper.readName(entryContainer);
            iconExpr        = MiniGameEntryHelper.readIcon(entryContainer);
            descriptionExpr = MiniGameEntryHelper.readDescription(entryContainer);
            authorExpr      = MiniGameEntryHelper.readAuthor(entryContainer);
            minPlayersExpr     = MiniGameEntryHelper.readMinPlayers(entryContainer);
            maxPlayersExpr     = MiniGameEntryHelper.readMaxPlayers(entryContainer);
            tagsExpr           = MiniGameEntryHelper.readTags(entryContainer);
            ch.njol.skript.config.SectionNode teamsSectionNode = MiniGameEntryHelper.readTeamsSectionNode(entryContainer);
            if (teamsSectionNode != null) {
                MiniGameEntryHelper.ParsedTeamsResult teamsResult = MiniGameEntryHelper.parseTeams(teamsSectionNode);
                if (teamsResult != null) {
                    parsedTeams = teamsResult.teams();
                    defaultTeamRules = teamsResult.defaultTeamRules();
                }
            }
            teamAssignmentExpr = MiniGameEntryHelper.readTeamAssignment(entryContainer);
            ch.njol.skript.config.SectionNode valuesSectionNode = MiniGameEntryHelper.readValuesSectionNode(entryContainer);
            if (valuesSectionNode != null) parsedValues = MiniGameEntryHelper.parseValues(valuesSectionNode);
            cancelEventsExpr  = MiniGameEntryHelper.readCancelEvents(entryContainer);
            timeExpr          = MiniGameEntryHelper.readTime(entryContainer);
            weatherExpr       = MiniGameEntryHelper.readWeather(entryContainer);
            instructionsExpr  = MiniGameEntryHelper.readInstructions(entryContainer);

            List<Node> unhandled = entryContainer.getUnhandledNodes();
            if (!unhandled.isEmpty()) {
                SectionNode source = entryContainer.getSource();
                List<Node> all = new ArrayList<>();
                for (Node node : source) all.add(node);
                for (Node node : all) source.remove(node);
                for (Node node : unhandled) source.add(node);

                ParserInstance parser = getParser();
                ParserInstance.Backup backup = parser.backup();
                parser.reset();
                parser.setCurrentEvent("register minigame", MiniGameRegisterEvent.class);
                SimpleEvent dummyEvent = new SimpleEvent();
                parser.setCurrentStructure(dummyEvent);
                List<TriggerItem> items = ScriptLoader.loadItems(source);
                parser.restoreBackup(backup);
                trigger = new Trigger(parser.getCurrentScript(), "register minigame " + id.getSingle(null), dummyEvent, items);
            }
        }
        return true;
    }

    @Override
    public boolean load() {
        return true;
    }

    @Override
    public boolean postLoad() {
        String minigameId = id.getSingle(null);
        if (minigameId == null) return false;
        MiniGame mg = miniGameManager.registerMiniGame(minigameId);
        MiniGameEntryHelper.apply(mg, nameExpr, iconExpr, descriptionExpr, authorExpr, minPlayersExpr, maxPlayersExpr, tagsExpr, parsedTeams, defaultTeamRules, teamAssignmentExpr, parsedValues, cancelEventsExpr, timeExpr, weatherExpr, instructionsExpr);
        // Always save after postLoad so that after a /sk reload the file contains this minigame
        // (unload() removes it via unregisterMiniGame; without this, a no-values minigame would never
        // be re-written to file, causing disabled-state loss on next server restart).
        miniGameManager.save();
        if (trigger != null) {
            MiniGameRegisterEvent registerEvent = new MiniGameRegisterEvent(mg);
            TriggerItem.walk(trigger, registerEvent);
        }
        return true;
    }

    @Override
    public void unload() {
        String minigameId = id.getSingle(null);
        if (minigameId == null) return;
        int kicked = 0;
        for (Session session : SessionManager.getInstance().getAllSessions()) {
            if (session.getMiniGame() != null && minigameId.equals(session.getMiniGame().getId())) {
                SessionLifecycleManagerImpl.getInstance().disbandSession(session, DisbandReason.EXPLICIT_DISBAND);
                kicked++;
            }
        }
        if (kicked > 0)
            SkGame.getInstance().getLogUtil().info("Reloaded minigame '" + minigameId + "': kicked " + kicked + " running session(s)");
        miniGameManager.unregisterMiniGame(minigameId);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "register minigame with id " + id.toString(event, debug);
    }
}
