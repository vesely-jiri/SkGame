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
@Name("Session - Persistent")
@Description({
        "Whether this session is exempt from automatic disbanding.",
        "When true, the session survives becoming empty and idle-timeout never fires.",
        "Explicit disband (delete session / admin force) still works normally.",
        "",
        "Intended for script-managed sessions created without a host.",
        "Set this inside the 'create game session' section block before players join.",
        "",
        "Supports: GET / SET."
})
@Examples({
        "create game session with id \"arena_1\":",
        "    set persistent of event-session to true",
        "    set session minigame of event-session to minigame with id \"bomberman\"",
        "",
        "if persistent of event-session is true:",
        "    broadcast \"Session is server-managed.\""
})
@Since("1.0.0")
public class ExprSessionPersistent extends SimplePropertyExpression<Session, Boolean> {

    static {
        register(ExprSessionPersistent.class, Boolean.class, "[session] persist[ent]", "session");
    }

    @Override
    public @Nullable Boolean convert(Session session) {
        return session.isPersistent();
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
        session.setPersistent((Boolean) delta[0]);
    }

    @Override
    protected String getPropertyName() {
        return "persistent";
    }

    @Override
    public Class<? extends Boolean> getReturnType() {
        return Boolean.class;
    }
}
