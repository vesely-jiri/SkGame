package cz.nox.skgame.skript.sections;

import ch.njol.skript.Skript;
import ch.njol.skript.ScriptLoader;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.registrations.Classes;
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.MinigameTag;
import cz.nox.skgame.api.game.model.TeamEntry;
import cz.nox.skgame.api.game.model.type.CancellableEventType;
import cz.nox.skgame.api.game.model.type.CustomValuePlurality;
import cz.nox.skgame.api.game.model.type.TeamAssignmentMode;
import cz.nox.skgame.core.game.MiniGameManager;
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
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared entry definitions and apply logic for register-minigame syntax elements.
 * Used by both StrucRegisterMiniGame (Structure, top-level) and EffSecRegisterMiniGame (Section, inside on load:).
 */
final class MiniGameEntryHelper {

    /** One entry from the values: section — gamemap or session scope value definition. */
    record ValueDefEntry(boolean isGamemap, String key, CustomValue cv) {}

    // key pattern: (gamemap|session) value "id"
    private static final Pattern VALUE_KEY_PATTERN =
            Pattern.compile("^(gamemap|session) value \"([^\"]+)\"$");

    // Individual constants so callers can use canCreateWith/getValue for manual iteration.
    static final ExpressionEntryData<String>             NAME_ENTRY            = new ExpressionEntryData<>("name",            null, true, String.class);
    static final ExpressionEntryData<ItemStack>          ICON_ENTRY            = new ExpressionEntryData<>("icon",            null, true, ItemStack.class);
    static final ExpressionEntryData<String>             DESCRIPTION_ENTRY     = new ExpressionEntryData<>("description",     null, true, String.class);
    static final ExpressionEntryData<String>             AUTHOR_ENTRY          = new ExpressionEntryData<>("author",          null, true, String.class);
    static final ExpressionEntryData<Number>             MIN_PLAYERS_ENTRY     = new ExpressionEntryData<>("min players",     null, true, Number.class);
    static final ExpressionEntryData<MinigameTag>        MINIGAME_TAGS_ENTRY   = new ExpressionEntryData<>("minigame tags",   null, true, MinigameTag.class);
    static final ExpressionEntryData<MinigameTag>        TAGS_ENTRY            = new ExpressionEntryData<>("tags",            null, true, MinigameTag.class);
    static final SectionEntryData                        TEAMS_SECTION_ENTRY      = new SectionEntryData("teams", null, true);
    static final ExpressionEntryData<TeamAssignmentMode> TEAM_ASSIGNMENT_ENTRY    = new ExpressionEntryData<>("team assignment", null, true, TeamAssignmentMode.class);
    static final SectionEntryData                        VALUES_SECTION_ENTRY     = new SectionEntryData("values", null, true);
    static final ExpressionEntryData<CancellableEventType> CANCEL_EVENTS_ENTRY   = new ExpressionEntryData<>("cancel events", null, true, CancellableEventType.class);

    /** Validator for an individual team body (name: / icon:). */
    static final EntryValidator TEAM_BODY_VALIDATOR = EntryValidator.builder()
            .addEntryData(new ExpressionEntryData<>("name", null, true, String.class))
            .addEntryData(new ExpressionEntryData<>("icon", null, true, ItemStack.class))
            .build();

    /** Validator for an individual value-def body (inside gamemap/session value "id":). */
    @SuppressWarnings("unchecked")
    static final EntryValidator VALUE_BODY_VALIDATOR = EntryValidator.builder()
            .addEntryData(new ExpressionEntryData<>("name",        null, true, String.class))
            .addEntryData(new ExpressionEntryData<>("type",        null, true, Object.class))
            .addEntryData(new ExpressionEntryData<>("default",     null, true, Object.class))
            .addEntryData(new ExpressionEntryData<>("plurality",   null, true, String.class))
            .addEntryData(new ExpressionEntryData<>("description", null, true, String.class))
            .addEntryData(new ExpressionEntryData<>("min",         null, true, Number.class))
            .addEntryData(new ExpressionEntryData<>("max",         null, true, Number.class))
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
            .addEntryData(VALUES_SECTION_ENTRY)
            .addEntryData(CANCEL_EVENTS_ENTRY)
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

    /** Returns the raw SectionNode for the "values:" entry, or null if absent. */
    static @Nullable SectionNode readValuesSectionNode(EntryContainer c) {
        Object raw = c.getOptional("values", false);
        return raw instanceof SectionNode sn ? sn : null;
    }

    @SuppressWarnings("unchecked")
    static @Nullable Expression<TeamAssignmentMode> readTeamAssignment(EntryContainer c) {
        return (Expression<TeamAssignmentMode>) c.getOptional("team assignment", false);
    }

    @SuppressWarnings("unchecked")
    static @Nullable Expression<CancellableEventType> readCancelEvents(EntryContainer c) {
        return (Expression<CancellableEventType>) c.getOptional("cancel events", false);
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

    /**
     * Parse a "values:" SectionNode into a list of ValueDefEntry.
     * Each child key must match: gamemap value "id" or session value "id".
     * Child body may contain: name, value type, default value, plurality, description.
     * Returns null (and logs an error) on parse failure. Returns empty list for empty section.
     */
    @SuppressWarnings("unchecked")
    static @Nullable List<ValueDefEntry> parseValues(SectionNode valuesSection) {
        List<ValueDefEntry> entries = new ArrayList<>();
        for (Node child : valuesSection) {
            if (child.getKey() == null) continue;
            String rawKey = ScriptLoader.replaceOptions(child.getKey());
            Matcher m = VALUE_KEY_PATTERN.matcher(rawKey);
            if (!m.matches()) {
                Skript.error("Invalid value definition key '" + rawKey
                        + "'. Expected: gamemap value \"id\" or session value \"id\"");
                return null;
            }
            boolean isGamemap = "gamemap".equals(m.group(1));
            String valueId = m.group(2);

            CustomValue cv = new CustomValue();
            if (child instanceof SectionNode valueSection) {
                EntryContainer body = VALUE_BODY_VALIDATOR.validate(valueSection);
                if (body == null) {
                    Skript.error("Invalid value definition for '" + rawKey + "': check entry syntax (type:, default:, name:, etc.)");
                    continue;
                }

                Expression<String> nameExpr = (Expression<String>) body.getOptional("name", false);
                if (nameExpr != null) cv.setName(nameExpr.getSingle(null));

                Expression<?> typeExpr = (Expression<?>) body.getOptional("type", false);
                if (typeExpr != null) {
                    Object typeVal = typeExpr.getSingle(null);
                    ClassInfo<?> ci = null;
                    if (typeVal instanceof ClassInfo<?> info) {
                        ci = info;
                    } else if (typeVal instanceof String s) {
                        String code = s.trim().toLowerCase(Locale.ROOT);
                        if ("text".equals(code)) code = "string";
                        ci = Classes.getClassInfoNoError(code);
                        if (ci == null)
                            Skript.warning("Unknown value type '" + s + "' in values: — type will be null");
                    }
                    cv.setType(ci);
                }

                Expression<Object> defExpr = (Expression<Object>) body.getOptional("default", false);
                if (defExpr != null) cv.setDefaultValue(defExpr.getSingle(null));

                Expression<String> plurExpr = (Expression<String>) body.getOptional("plurality", false);
                if (plurExpr != null) {
                    String plur = plurExpr.getSingle(null);
                    if (plur != null) {
                        try {
                            cv.setPlurality(CustomValuePlurality.valueOf(plur.toUpperCase(Locale.ROOT)));
                        } catch (IllegalArgumentException ignored) {
                            Skript.warning("Unknown plurality '" + plur + "' — expected 'single' or 'plural'");
                        }
                    }
                }

                Expression<String> descExpr = (Expression<String>) body.getOptional("description", false);
                if (descExpr != null) cv.setDescription(descExpr.getSingle(null));

                boolean isNumericType = cv.getType() != null
                        && (Number.class.isAssignableFrom(cv.getType().getC()));
                Expression<Number> minExpr = (Expression<Number>) body.getOptional("min", false);
                if (minExpr != null) {
                    if (!isNumericType) Skript.warning("'min' bound ignored: value type is not numeric for key '" + valueId + "'");
                    else cv.setMinValue(minExpr.getSingle(null));
                }
                Expression<Number> maxExpr = (Expression<Number>) body.getOptional("max", false);
                if (maxExpr != null) {
                    if (!isNumericType) Skript.warning("'max' bound ignored: value type is not numeric for key '" + valueId + "'");
                    else cv.setMaxValue(maxExpr.getSingle(null));
                }
            }
            entries.add(new ValueDefEntry(isGamemap, valueId, cv));
        }
        return entries;
    }

    static void apply(MiniGame mg,
                      @Nullable Expression<String>               nameExpr,
                      @Nullable Expression<ItemStack>            iconExpr,
                      @Nullable Expression<String>               descriptionExpr,
                      @Nullable Expression<String>               authorExpr,
                      @Nullable Expression<Number>               minPlayersExpr,
                      @Nullable Expression<MinigameTag>          tagsExpr,
                      @Nullable List<TeamEntry>                  parsedTeams,
                      @Nullable Expression<TeamAssignmentMode>   teamAssignmentExpr,
                      @Nullable List<ValueDefEntry>              parsedValues,
                      @Nullable Expression<CancellableEventType> cancelEventsExpr) {
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
        if (parsedValues != null && !parsedValues.isEmpty()) {
            for (ValueDefEntry ve : parsedValues) {
                if (ve.isGamemap()) mg.setGameMapValueDef(ve.key(), ve.cv());
                else mg.setSessionValueDef(ve.key(), ve.cv());
            }
            MiniGameManager.getInstance().save();
        }
        if (cancelEventsExpr != null) {
            CancellableEventType[] types = cancelEventsExpr.getArray(null);
            if (types.length > 0) {
                Set<CancellableEventType> set = EnumSet.copyOf(Arrays.asList(types));
                mg.setCancelledEvents(set);
            }
        }
    }

    private MiniGameEntryHelper() {}
}
