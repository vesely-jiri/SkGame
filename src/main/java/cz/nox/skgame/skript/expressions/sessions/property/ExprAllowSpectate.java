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
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - allow spectate")
@Description({
        "Whether spectators may join this session via the spectate browser or /game spectate.",
        "Host can toggle this per-session; default is controlled by spectate.default-allow in config.yml.",
        "",
        "Supports: GET / SET."
})
@Examples({
        "set allow spectate of event-session to false",
        "if allow spectate of {_session} is true:",
        "    open spectate gui for player"
})
@Since("1.0.0")
public class ExprAllowSpectate extends SimplePropertyExpression<Session, Boolean> {

    static {
        register(ExprAllowSpectate.class, Boolean.class, "[session] allow spectate", "session");
    }

    @Override
    public @Nullable Boolean convert(Session session) {
        return session.isAllowSpectate();
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
        session.setAllowSpectate((Boolean) delta[0]);
        Bukkit.getPluginManager().callEvent(new SessionSettingsChangedEvent(session, "allow-spectate"));
    }

    @Override
    protected String getPropertyName() {
        return "allow spectate";
    }

    @Override
    public Class<? extends Boolean> getReturnType() {
        return Boolean.class;
    }
}
