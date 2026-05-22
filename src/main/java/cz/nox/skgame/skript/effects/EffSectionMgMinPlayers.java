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

@Name("Minigame Section — Min Players")
@Description({
        "Inline shorthand for setting a minigame's minimum player count inside a 'register minigame' section.",
        "Equivalent to: set min players of event-minigame to ...",
        "No-op when used outside a minigame registration section."
})
@Examples({
        "register new minigame with id \"koth\":",
        "    name: \"King of the Hill\"",
        "    author: \"JuraJ_Player\"",
        "    min players: 2"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSectionMgMinPlayers extends Effect {

    private Expression<Number> value;

    static {
        Skript.registerEffect(EffSectionMgMinPlayers.class, "min players: %number%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.value = (Expression<Number>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (!(event instanceof MiniGameRegisterEvent e)) return;
        Number n = value.getSingle(event);
        if (n == null) return;
        e.getMiniGame().setValue("min_players", n.longValue());
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "min players: " + value.toString(event, b);
    }
}
