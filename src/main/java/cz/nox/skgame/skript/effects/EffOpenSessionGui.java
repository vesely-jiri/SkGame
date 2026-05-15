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
import cz.nox.skgame.core.gui.services.SessionGuiService;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("GUI - Open Session GUI")
@Description("Opens the session lobby GUI for one or more players.")
@Examples("open session gui to player")
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffOpenSessionGui extends Effect {

    private Expression<Player> players;

    static {
        Skript.registerEffect(EffOpenSessionGui.class, "open session gui to %players%");
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
            SessionGuiService.getInstance().openFor(player);
        }
    }

    @Override
    public String toString(@Nullable Event event, boolean b) {
        return "open session gui to " + this.players.toString(event, b);
    }
}
