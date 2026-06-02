package cz.nox.skgame.skript.expressions.minigames;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.MinigameTag;
import cz.nox.skgame.util.Debug;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

@Name("Minigame Tags")
@Description({
        "The tags of a minigame.",
        "Valid tag names: pvp, pve, ffa, team, building, puzzle, race (case-insensitive).",
        "",
        "Supports: GET / SET / ADD / REMOVE / DELETE."
})
@Examples({
        "set tags of event-minigame to \"pvp\", \"team\"",
        "add \"ffa\" to tags of event-minigame",
        "remove \"pvp\" from tags of event-minigame",
        "delete tags of event-minigame",
        "set {_tags::*} to tags of event-minigame",
        "if tags of {_mg} contains \"pvp\":"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprMiniGameTags extends SimpleExpression<MinigameTag> {

    private Expression<MiniGame> minigameExpr;

    static {
        Skript.registerExpression(ExprMiniGameTags.class, MinigameTag.class, ExpressionType.PROPERTY,
                "[the] [minigame] tags of %minigame%",
                "%minigame%'[s] tags"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean kleenean,
                        SkriptParser.ParseResult parseResult) {
        minigameExpr = (Expression<MiniGame>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable MinigameTag[] get(Event event) {
        MiniGame mg = minigameExpr.getSingle(event);
        if (mg == null) return new MinigameTag[0];
        return mg.getTags().toArray(new MinigameTag[0]);
    }

    @Override
    public @Nullable Class<?>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET           -> CollectionUtils.array(String[].class);
            case ADD, REMOVE   -> CollectionUtils.array(String.class);
            case DELETE, RESET -> CollectionUtils.array();
            default            -> null;
        };
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        MiniGame mg = minigameExpr.getSingle(event);
        if (mg == null) return;
        switch (mode) {
            case SET -> {
                Set<MinigameTag> tags = EnumSet.noneOf(MinigameTag.class);
                if (delta != null) for (Object o : delta) parseTag(o, tags::add);
                mg.setTags(tags);
            }
            case ADD    -> { if (delta != null) for (Object o : delta) parseTag(o, mg::addTag); }
            case REMOVE -> { if (delta != null) for (Object o : delta) parseTag(o, mg::removeTag); }
            case DELETE, RESET -> mg.setTags(EnumSet.noneOf(MinigameTag.class));
        }
    }

    private static void parseTag(Object o, Consumer<MinigameTag> consumer) {
        if (!(o instanceof String s)) return;
        try {
            consumer.accept(MinigameTag.valueOf(s.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            Debug.log("minigame-tags", "unknown tag: \"" + s + "\" — valid: pvp, pve, ffa, team, building, puzzle, race");
        }
    }

    @Override
    public boolean isSingle() {
        return false;
    }

    @Override
    public Class<? extends MinigameTag> getReturnType() {
        return MinigameTag.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "tags of " + minigameExpr.toString(event, debug);
    }
}
