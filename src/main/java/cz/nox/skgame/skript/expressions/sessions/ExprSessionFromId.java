package cz.nox.skgame.skript.expressions.sessions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.core.game.SessionManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

@Name("Session - by ID")
@Description({
        "Retrieves one or more sessions using their UUID(s).",
        "Existing sessions are not overwritten.",
        "If the session does not exist and the 'new' tag is used, a new session with the specified UUID will be created.",
        "",
        "Supports: GET only (with optional creation)."
})
@Examples({
        "set {_s} to session with id \"my_uuid\"",
        "",
        "set {_new} to new session with id \"new_uuid\"",
        "",
        "loop {_sessions::*}:",
        "    broadcast \"Session: %id of loop-value%\""
})
@Since("1.0.0")
@SuppressWarnings("unused")
public class ExprSessionFromId extends SimpleExpression<Session> {

    private static final SessionManager sessionManager = SessionManager.getInstance();
    private Expression<String> uuids;
    private boolean create;

    static {
        Skript.registerExpression(ExprSessionFromId.class, Session.class, ExpressionType.COMBINED,
                "[:new] session[s] (with|from) [[uu]id][s] %strings%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.uuids = (Expression<String>) exprs[0];
        this.create = parseResult.hasTag("new");
        return true;
    }

    @Override
    protected @Nullable Session[] get(Event event) {
        return Arrays.stream(this.uuids.getArray(event))
                .map(id -> {
                    Session session = sessionManager.getSessionById(id);
                    if (session == null && this.create) {
                        session = sessionManager.createSession(id);
                    }
                    return session;
                })
                .filter(Objects::nonNull)
                .toArray(Session[]::new);
    }

    @Override
    public Class<? extends Session> getReturnType() {
        return Session.class;
    }

    @Override
    public boolean isSingle() {
        return this.uuids.isSingle();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "session[s] with id[s] " + this.uuids.toString(event, debug);
    }
}
