package cz.nox.skgame.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.ScriptLoader;
import ch.njol.skript.lang.Expression;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.MinigameTag;
import cz.nox.skgame.api.game.model.TeamEntry;
import cz.nox.skgame.api.game.model.type.TeamAssignmentMode;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.SectionEntryData;
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Shared entry definitions and apply logic for register-minigame syntax elements.
 * Used by both StrucRegisterMiniGame (Structure, top-level) and EffSecRegisterMiniGame (Section, inside on load:).
 */
final class MiniGameEntryHelper {

    // Individual constants so callers can use canCreateWith/getValue for manual iteration.
    static final ExpressionEntryData<String>             NAME_ENTRY            = new ExpressionEntryData<>("name",            null, true, String.class);
    static final ExpressionEntryData<ItemStack>          ICON_ENTRY            = new ExpressionEntryData<>("icon",            null, true, ItemStack.class);
    static final ExpressionEntryData<String>             DESCRIPTION_ENTRY     = new ExpressionEntryData<>("description",     null, true, String.class);
    static final ExpressionEntryData<String>             AUTHOR_ENTRY          = new ExpressionEntryData<>("author",          null, true, String.class);
    static final ExpressionEntryData<Number>             MIN_PLAYERS_ENTRY     = new ExpressionEntryData<>("min players",     null, true, Number.class);
    static final ExpressionEntryData<MinigameTag>        MINIGAME_TAGS_ENTRY   = new ExpressionEntryData<>("minigame tags",   null, true, MinigameTag.class);
    static final ExpressionEntryData<MinigameTag>        TAGS_ENTRY            = new ExpressionEntryData<>("tags",            null, true, MinigameTag.class);
    static final SectionEntryData                        TEAMS_SECTION_ENTRY   = new SectionEntryData("teams", null, true);
    static final ExpressionEntryData<TeamAssignmentMode> TEAM_ASSIGNMENT_ENTRY = new ExpressionEntryData<>("team assignment", null, true, TeamAssignmentMode.class);

    /** Validator for an individual team body (name: / icon:). */
    static final EntryValidator TEAM_BODY_VALIDATOR = EntryValidator.builder()
            .addEntryData(new ExpressionEntryData<>("name", null, true, String.class))
            .addEntryData(new ExpressionEntryData<>("icon", null, true, ItemStack.class))
            .build();

    /**
     * MIXED validator — passed to Skript.registerStructure() and used manually in EffSecRegisterMiniGame.
     * Unknown nodes (free-form effects/sections) route to EntryContainer.getUnhandledNodes() without error.
     */
    static final EntryValidator MIXED = EntryValidator.builder()
            .addEntryData(NAME_ENTRY)
            .addEntryData(ICON_ENTRY)
            .addEntryData(DESCRIPTION_ENTRY)
            .addEntryData(AUTHOR_ENTRY)
            .addEntryData(MIN_PLAYERS_ENTRY)
            .addEntryData(MINIGAME_TAGS_ENTRY)
            .addEntryData(TAGS_ENTRY)
            .addEntryData(TEAMS_SECTION_ENTRY)
            .addEntryData(TEAM_ASSIGNMENT_ENTRY)
            .unexpectedNodeTester(node -> false)
            .build();

    @SuppressWarnings("unchecked")
    static @Nullable Expression<String> readName(EntryContainer c) {
        return (Expression<String>) c.getOptional("name", false);
    }

    @SuppressWarnings("unchecked")
    static @Nullable Expression<ItemStack> readIcon(EntryContainer c) {
        return (Expression<ItemStack>) c.getOptional("icon", false);
    }

    @SuppressWarnings("unchecked")
    static @Nullable Expression<String> readDescription(EntryContainer c) {
        return (Expression<String>) c.getOptional("description", false);
    }

    @SuppressWarnings("unchecked")
    static @Nullable Expression<String> readAuthor(EntryContainer c) {
        return (Expression<String>) c.getOptional("author", false);
    }

    @SuppressWarnings("unchecked")
    static @Nullable Expression<Number> readMinPlayers(EntryContainer c) {
        return (Expression<Number>) c.getOptional("min players", false);
    }

    @SuppressWarnings("unchecked")
    static @Nullable Expression<MinigameTag> readTags(EntryContainer c) {
        Expression<MinigameTag> v = (Expression<MinigameTag>) c.getOptional("tags", false);
        if (v == null) v = (Expression<MinigameTag>) c.getOptional("minigame tags", false);
        return v;
    }

    /** Returns the raw SectionNode for the "teams:" entry, or null if absent. */
    static @Nullable SectionNode readTeamsSectionNode(EntryContainer c) {
        Object raw = c.getOptional("teams", false);
        return raw instanceof SectionNode sn ? sn : null;
    }

    @SuppressWarnings("unchecked")
    static @Nullable Expression<TeamAssignmentMode> readTeamAssignment(EntryContainer c) {
        return (Expression<TeamAssignmentMode>) c.getOptional("team assignment", false);
    }

    /**
     * Parse a "teams:" SectionNode into a list of TeamEntry.
     * Each child node's key is the team id; if the child is a SectionNode its body
     * may contain "name:" and "icon:" entries. Returns null (and logs an error) on
     * duplicate team ids. Returns empty list for an empty section.
     */
    @SuppressWarnings("unchecked")
    static @Nullable List<TeamEntry> parseTeams(SectionNode teamsSection) {
        List<TeamEntry> entries = new ArrayList<>();
        LinkedHashSet<String> seenIds = new LinkedHashSet<>();
        for (Node child : teamsSection) {
            if (child.getKey() == null) continue;
            String teamId = ScriptLoader.replaceOptions(child.getKey());
            if (!seenIds.add(teamId)) {
                Skript.error("Duplicate team id in register block: \"" + teamId + "\"");
                return null;
            }
            String displayName = null;
            ItemStack icon = null;
            if (child instanceof SectionNode teamSection) {
                EntryContainer teamBody = TEAM_BODY_VALIDATOR.validate(teamSection);
                if (teamBody != null) {
                    Expression<String> nameExpr = (Expression<String>) teamBody.getOptional("name", false);
                    if (nameExpr != null) displayName = nameExpr.getSingle(null);
                    Expression<ItemStack> iconExpr = (Expression<ItemStack>) teamBody.getOptional("icon", false);
                    if (iconExpr != null) {
                        ItemStack v = iconExpr.getSingle(null);
                        if (v != null) icon = v;
                    }
                }
            }
            // non-section child = id-only (name/icon default)
            entries.add(new TeamEntry(teamId, displayName, icon));
        }
        return entries;
    }

    static void apply(MiniGame mg,
                      @Nullable Expression<String>             nameExpr,
                      @Nullable Expression<ItemStack>          iconExpr,
                      @Nullable Expression<String>             descriptionExpr,
                      @Nullable Expression<String>             authorExpr,
                      @Nullable Expression<Number>             minPlayersExpr,
                      @Nullable Expression<MinigameTag>        tagsExpr,
                      @Nullable List<TeamEntry>                parsedTeams,
                      @Nullable Expression<TeamAssignmentMode> teamAssignmentExpr) {
        if (nameExpr != null) {
            String v = nameExpr.getSingle(null);
            if (v != null) mg.setValue("name", v);
        }
        if (iconExpr != null) {
            ItemStack v = iconExpr.getSingle(null);
            if (v != null) mg.setValue("icon", v);
        }
        if (descriptionExpr != null) {
            String v = descriptionExpr.getSingle(null);
            if (v != null) mg.setValue("description", v);
        }
        if (authorExpr != null) {
            String v = authorExpr.getSingle(null);
            if (v != null) mg.setValue("author", v);
        }
        if (minPlayersExpr != null) {
            Number v = minPlayersExpr.getSingle(null);
            if (v != null) mg.setValue("min_players", v.longValue());
        }
        if (tagsExpr != null) {
            MinigameTag[] tags = tagsExpr.getArray(null);
            if (tags.length > 0) mg.setTags(EnumSet.copyOf(Arrays.asList(tags)));
        }
        if (parsedTeams != null) {
            mg.setTeamEntries(parsedTeams);
        }
        if (teamAssignmentExpr != null) {
            TeamAssignmentMode mode = teamAssignmentExpr.getSingle(null);
            if (mode != null) mg.setTeamAssignment(mode);
        }
    }

    private MiniGameEntryHelper() {}
}
