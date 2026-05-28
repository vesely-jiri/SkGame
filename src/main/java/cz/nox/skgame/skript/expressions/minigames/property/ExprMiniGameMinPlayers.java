package cz.nox.skgame.skript.expressions.minigames.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.MiniGame;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@Name("MiniGame - Min Players")
@Description({
        "The minimum number of players required to start a minigame.",
        "",
        "Supports: GET / SET / RESET."
})
@Examples({
        "set min players of event-minigame to 2",
        "if min players of minigame with id \"bomberman\" > 4:"
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprMiniGameMinPlayers extends SimplePropertyExpression<MiniGame, Number> {

    static {
        register(ExprMiniGameMinPlayers.class, Number.class, "min[imum] [minigame] players", "minigame");
    }

    @Override
    public @Nullable Number convert(MiniGame miniGame) {
        Object v = miniGame.getValue("min_players");
        if (v instanceof Number n) return n;
        return null;
    }

    @Override
    public @Nullable Class<? extends Number>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> CollectionUtils.array(Number.class);
            case RESET, DELETE -> CollectionUtils.array();
            default -> null;
        };
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        MiniGame mg = getExpr().getSingle(event);
        if (mg == null) return;
        switch (mode) {
            case SET -> {
                if (delta == null || delta[0] == null) return;
                mg.setValue("min_players", ((Number) delta[0]).longValue());
            }
            case RESET, DELETE -> mg.removeValue("min_players");
        }
    }

    @Override
    protected String getPropertyName() {
        return "min players";
    }

    @Override
    public Class<? extends Number> getReturnType() {
        return Number.class;
    }
}
