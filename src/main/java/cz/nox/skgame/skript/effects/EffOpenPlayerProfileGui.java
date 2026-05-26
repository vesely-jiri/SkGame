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
import cz.nox.skgame.core.gui.services.PlayerProfileGuiService;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("Open Player Profile GUI")
@Description("Opens the player profile stats GUI. Subject defaults to viewer if omitted.")
@Examples({
        "open player profile gui for player",
        "open player profile gui of Notch for player"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class EffOpenPlayerProfileGui extends Effect {

    private Expression<Player> viewer;
    private Expression<OfflinePlayer> subject;

    static {
        Skript.registerEffect(EffOpenPlayerProfileGui.class,
                "open [player] profile [gui] [of %-offlineplayer%] for %player%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean k, SkriptParser.ParseResult pr) {
        subject = (Expression<OfflinePlayer>) exprs[0];
        viewer  = (Expression<Player>) exprs[1];
        return true;
    }

    @Override
    protected void execute(Event event) {
        Player v = viewer.getSingle(event);
        if (v == null) return;
        OfflinePlayer s = (subject != null) ? subject.getSingle(event) : null;
        PlayerProfileGuiService.getInstance().openFor(v, s != null ? s : v);
    }

    @Override
    public String toString(@Nullable Event e, boolean d) {
        return "open player profile gui for " + viewer.toString(e, d);
    }
}
