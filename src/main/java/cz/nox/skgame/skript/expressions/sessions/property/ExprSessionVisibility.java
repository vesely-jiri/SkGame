package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.classes.Changer;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import ch.njol.util.coll.CollectionUtils;
import cz.nox.skgame.api.game.event.SessionSettingsChangedEvent;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.SessionVisibility;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - Visibility")
@Description({
        "The visibility of a session: PUBLIC (default) or PRIVATE.",
        "Private sessions are hidden from the main GUI session list and tab-completion of /game join.",
        "Only the host should change visibility; no permission guard is enforced at the Skript level.",
        "",
        "Supports: GET / SET."
})
@Examples({
        "set session visibility of event-session to private",
        "if session visibility of event-session is public:",
        "    broadcast \"Session is joinable!\""
})
@Since("1.0.0")
public class ExprSessionVisibility extends SimplePropertyExpression<Session, SessionVisibility> {

    static {
        register(ExprSessionVisibility.class, SessionVisibility.class, "[session] visibility", "session");
    }

    @Override
    public @Nullable SessionVisibility convert(Session session) {
        return session.getVisibility();
    }

    @Override
    public @Nullable Class<? extends SessionVisibility>[] acceptChange(Changer.ChangeMode mode) {
        return switch (mode) {
            case SET -> CollectionUtils.array(SessionVisibility.class);
            default  -> null;
        };
    }

    @Override
    public void change(Event event, @Nullable Object[] delta, Changer.ChangeMode mode) {
        Session session = getExpr().getSingle(event);
        if (session == null || delta == null || delta[0] == null) return;
        session.setVisibility((SessionVisibility) delta[0]);
        Bukkit.getPluginManager().callEvent(new SessionSettingsChangedEvent(session, "visibility"));
    }

    @Override
    protected String getPropertyName() { return "visibility"; }

    @Override
    public Class<? extends SessionVisibility> getReturnType() { return SessionVisibility.class; }
}
