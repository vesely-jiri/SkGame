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
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

@Name("Minigame Section — Icon")
@Description({
        "Inline shorthand for setting a minigame's GUI icon inside a 'register minigame' section.",
        "Equivalent to: set minigame icon of event-minigame to ...",
        "No-op when used outside a minigame registration section."
})
@Examples({
        "register new minigame with id \"bomberman\":",
        "    name: \"&cBomberMan\"",
        "    icon: TNT",
        "    min players: 2"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffSectionMgIcon extends Effect {

    private Expression<ItemStack> value;

    static {
        Skript.registerEffect(EffSectionMgIcon.class, "icon: %itemstack%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.value = (Expression<ItemStack>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        if (!(event instanceof MiniGameRegisterEvent e)) return;
        ItemStack item = value.getSingle(event);
        if (item == null) return;
        e.getMiniGame().setValue("icon", item);
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "icon: " + value.toString(event, b);
    }
}
