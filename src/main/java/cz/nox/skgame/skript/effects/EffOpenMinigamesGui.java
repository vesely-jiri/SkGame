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
import cz.nox.skgame.core.gui.services.MinigamesGuiService;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("GUI - Open Minigames GUI")
@Description("Opens the minigame selection GUI for one or more players.")
@Examples("open minigames gui to player")
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffOpenMinigamesGui extends Effect {

    private Expression<Player> players;

    static {
        Skript.registerEffect(EffOpenMinigamesGui.class, "open minigames gui to %players%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.players = (Expression<Player>) exprs[0];
        return true;
    }

    @Override
    protected void execute(Event event) {
        for (Player player : this.players.getAll(event)) {
            MinigamesGuiService.getInstance().openFor(player);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "open minigames gui to " + this.players.toString(event, b);
    }
}
