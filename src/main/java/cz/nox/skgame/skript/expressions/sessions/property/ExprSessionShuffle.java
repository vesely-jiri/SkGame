package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - Shuffle")
@Description({
        "Whether spawn order is randomized at game start for this session.",
        "When true, ExprShuffledSessionPlayers returns players in random order.",
        "",
        "Supports: GET / SET."
})
@Examples({
        "set shuffle of event-session to true",
        "if shuffle of event-session is true:",
        "    loop shuffled session players of event-session:"
})
@Since("1.0.0")
public class ExprSessionShuffle extends SimplePropertyExpression<Session, Boolean> {

    static {
        register(ExprSessionShuffle.class, Boolean.class, "shuffle", "session");
    }

    @Override
    public @Nullable Boolean convert(Session session) {
        return session.isShuffle();
    }

    @Override
    public @Nullable Class<? extends Boolean>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> CollectionUtils.array(Boolean.class);
            default  -> null;
        };
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Session session = getExpr().getSingle(event);
        if (session == null || delta == null || delta[0] == null) return;
        session.setShuffle((Boolean) delta[0]);
    }

    @Override
    protected String getPropertyName() {
        return "shuffle";
    }

    @Override
    public Class<? extends Boolean> getReturnType() {
        return Boolean.class;
    }
}
