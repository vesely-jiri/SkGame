package cz.nox.skgame.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
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
import cz.nox.skgame.api.game.model.MinigameTag;
import cz.nox.skgame.core.game.MiniGameManager;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Name("Register MiniGame")
@Description({
        "Registers a new MiniGame with a specified ID inside an event handler.",
        "The optional body supports entry shorthand (name, icon, description, author, min players, minigame tags)",
        "and free-form effects (set minigame name of event-minigame to ...) — both can be mixed.",
        "",
        "Supports: event-minigame"
})
@Examples({
        "on load:",
        "    register minigame with id \"koth\":",
        "        name: \"King of the Hill\"",
        "        icon: red banner",
        "        min players: 2",
        "        minigame tags: PVP, Team",
        "",
        "on load:",
        "    register minigame with id \"bomberman\":",
        "        name: \"&cBomberMan\"",
        "        set gamemap value \"gamemode\" of event-minigame to a custom value:",
        "            set value name to \"Gamemode\"",
        "            set value type to a number"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSecRegisterMiniGame extends EffectSection {

    private static final MiniGameManager miniGameManager = MiniGameManager.getInstance();

    private Expression<String> id;
    private @Nullable Trigger trigger;

    private @Nullable Expression<String>      nameExpr;
    private @Nullable Expression<ItemStack>   iconExpr;
    private @Nullable Expression<String>      descriptionExpr;
    private @Nullable Expression<String>      authorExpr;
    private @Nullable Expression<Number>      minPlayersExpr;
    private @Nullable Expression<MinigameTag> tagsExpr;

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
                        SectionNode sectionNode, @Nullable List<TriggerItem> list) {
        this.id = (Expression<String>) exprs[0];

        if (hasSection()) {
            List<Node> freeForm = new ArrayList<>();
            for (Node node : sectionNode) {
                if (MiniGameEntryHelper.NAME_ENTRY.canCreateWith(node))
                    nameExpr = (Expression<String>) MiniGameEntryHelper.NAME_ENTRY.getValue(node);
                else if (MiniGameEntryHelper.ICON_ENTRY.canCreateWith(node))
                    iconExpr = (Expression<ItemStack>) MiniGameEntryHelper.ICON_ENTRY.getValue(node);
                else if (MiniGameEntryHelper.DESCRIPTION_ENTRY.canCreateWith(node))
                    descriptionExpr = (Expression<String>) MiniGameEntryHelper.DESCRIPTION_ENTRY.getValue(node);
                else if (MiniGameEntryHelper.AUTHOR_ENTRY.canCreateWith(node))
                    authorExpr = (Expression<String>) MiniGameEntryHelper.AUTHOR_ENTRY.getValue(node);
                else if (MiniGameEntryHelper.MIN_PLAYERS_ENTRY.canCreateWith(node))
                    minPlayersExpr = (Expression<Number>) MiniGameEntryHelper.MIN_PLAYERS_ENTRY.getValue(node);
                else if (MiniGameEntryHelper.TAGS_ENTRY.canCreateWith(node))
                    tagsExpr = (Expression<MinigameTag>) MiniGameEntryHelper.TAGS_ENTRY.getValue(node);
                else if (MiniGameEntryHelper.MINIGAME_TAGS_ENTRY.canCreateWith(node)) {
                    if (tagsExpr == null)
                        tagsExpr = (Expression<MinigameTag>) MiniGameEntryHelper.MINIGAME_TAGS_ENTRY.getValue(node);
                } else {
                    freeForm.add(node);
                }
            }
            if (!freeForm.isEmpty()) {
                List<Node> all = new ArrayList<>();
                for (Node node : sectionNode) all.add(node);
                for (Node node : all) sectionNode.remove(node);
                for (Node node : freeForm) sectionNode.add(node);
                trigger = loadCode(sectionNode, "minigame register", MiniGameRegisterEvent.class);
            }
        }

        return true;
    }

    @Override
    protected @Nullable TriggerItem walk(Event event) {
        Object localVars = Variables.copyLocalVariables(event);
        String minigameId = this.id.getSingle(event);
        if (minigameId == null) return super.walk(event, false);

        MiniGame mg = miniGameManager.registerMiniGame(minigameId);

        MiniGameEntryHelper.apply(mg, nameExpr, iconExpr, descriptionExpr, authorExpr, minPlayersExpr, tagsExpr);

        if (trigger != null) {
            MiniGameRegisterEvent registerEvent = new MiniGameRegisterEvent(mg);
            Variables.setLocalVariables(registerEvent, localVars);
            TriggerItem.walk(trigger, registerEvent);
            Variables.setLocalVariables(event, Variables.copyLocalVariables(registerEvent));
            Variables.removeLocals(registerEvent);
        }

        return super.walk(event, false);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "register minigame with id " + id.getSingle(event);
    }
}
