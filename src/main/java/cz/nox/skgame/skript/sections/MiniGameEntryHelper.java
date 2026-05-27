package cz.nox.skgame.skript.sections;

import ch.njol.skript.lang.Expression;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.MinigameTag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.entry.EntryValidator;
import org.skriptlang.skript.lang.entry.util.ExpressionEntryData;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * Shared entry definitions and apply logic for register-minigame syntax elements.
 * Used by both StrucRegisterMiniGame (Structure, top-level) and EffSecRegisterMiniGame (Section, inside on load:).
 */
final class MiniGameEntryHelper {

    // Individual constants so callers can use canCreateWith/getValue for manual iteration.
    static final ExpressionEntryData<String>     NAME_ENTRY          = new ExpressionEntryData<>("name",          null, true, String.class);
    static final ExpressionEntryData<ItemStack>  ICON_ENTRY          = new ExpressionEntryData<>("icon",          null, true, ItemStack.class);
    static final ExpressionEntryData<String>     DESCRIPTION_ENTRY   = new ExpressionEntryData<>("description",   null, true, String.class);
    static final ExpressionEntryData<String>      AUTHOR_ENTRY        = new ExpressionEntryData<>("author",        null, true, String.class);
    static final ExpressionEntryData<Number>      MIN_PLAYERS_ENTRY   = new ExpressionEntryData<>("min players",   null, true, Number.class);
    static final ExpressionEntryData<MinigameTag> MINIGAME_TAGS_ENTRY = new ExpressionEntryData<>("minigame tags", null, true, MinigameTag.class);
    static final ExpressionEntryData<MinigameTag> TAGS_ENTRY          = new ExpressionEntryData<>("tags",          null, true, MinigameTag.class);

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

    static void apply(MiniGame mg,
                      @Nullable Expression<String>      nameExpr,
                      @Nullable Expression<ItemStack>   iconExpr,
                      @Nullable Expression<String>      descriptionExpr,
                      @Nullable Expression<String>      authorExpr,
                      @Nullable Expression<Number>      minPlayersExpr,
                      @Nullable Expression<MinigameTag> tagsExpr) {
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
    }

    private MiniGameEntryHelper() {}
}
