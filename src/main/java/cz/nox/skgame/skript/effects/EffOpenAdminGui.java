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
import cz.nox.skgame.core.gui.services.AdminGuiService;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("GUI - Open Admin GUI")
@Description("Opens the admin GUI for one or more players.")
@Examples("open admin gui to player")
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffOpenAdminGui extends Effect {

    private Expression<Player> players;

    static {
        Skript.registerEffect(EffOpenAdminGui.class, "open admin gui to %players%");
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
            AdminGuiService.getInstance().openAdminGui(player);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "open admin gui to " + this.players.toString(event, b);
    }
}
