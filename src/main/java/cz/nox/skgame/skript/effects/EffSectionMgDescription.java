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
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Minigame Section — Description")
@Description({
        "Inline shorthand for setting a minigame's description inside a 'register minigame' section.",
        "Equivalent to: set minigame description of event-minigame to ...",
        "For multi-line descriptions use the full expression form with a list variable.",
        "No-op when used outside a minigame registration section."
})
@Examples({
        "register new minigame with id \"koth\":",
        "    name: \"King of the Hill\"",
        "    description: \"&7Hold the hill to score points\"",
        "    min players: 2"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSectionMgDescription extends Effect {

    private Expression<String> value;

    static {
        Skript.registerEffect(EffSectionMgDescription.class, "description: %string%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.value = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (!(event instanceof MiniGameRegisterEvent e)) return;
        String desc = value.getSingle(event);
        if (desc == null) return;
        e.getMiniGame().setValue("description", desc);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "description: " + value.toString(event, b);
    }
}
