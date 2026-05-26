package cz.nox.skgame.skript.effects;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Effect;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.event.MiniGameRegisterEvent;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.MinigameTag;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

@Name("Set Minigame Tags")
@Description({
        "Sets the tags of a minigame from a list of tag name strings.",
        "Use the inline form ('tags: ...') inside a register minigame section.",
        "Valid tag names: pvp, pve, ffa, team, building, puzzle, race (case-insensitive)."
})
@Examples({
        "register new minigame with id \"koth\":",
        "    tags: \"pvp\", \"team\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSetMiniGameTags extends Effect {

    private Expression<String> tagsExpr;
    private Expression<MiniGame> minigameExpr;
    private int pattern;

    static {
        Skript.registerEffect(EffSetMiniGameTags.class,
                "tags: %strings%",
                "set [minigame] tags of %minigame% to %strings%"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean kleenean,
                        SkriptParser.ParseResult parseResult) {
        this.pattern = matchedPattern;
        if (matchedPattern == 0) {
            tagsExpr = (Expression<String>) exprs[0];
        } else {
            minigameExpr = (Expression<MiniGame>) exprs[0];
            tagsExpr = (Expression<String>) exprs[1];
        }
        return true;
    }

    @Override
    protected void execute(Event event) {
        MiniGame mg;
        if (pattern == 0) {
            if (!(event instanceof MiniGameRegisterEvent e)) return;
            mg = e.getMiniGame();
        } else {
            mg = minigameExpr.getSingle(event);
        }
        if (mg == null) return;

        String[] tagNames = tagsExpr.getArray(event);
        Set<MinigameTag> tags = EnumSet.noneOf(MinigameTag.class);
        for (String name : tagNames) {
            try {
                tags.add(MinigameTag.valueOf(name.trim().toUpperCase()));
            } catch (IllegalArgumentException ignored) {}
        }
        mg.setTags(tags);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "set minigame tags to " + tagsExpr.toString(event, debug);
    }
}
