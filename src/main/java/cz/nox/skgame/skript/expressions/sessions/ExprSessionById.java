package cz.nox.skgame.skript.expressions.sessions;

import ch.njol.skript.Skript;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
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

@Name("Session by UUID")
@Description("Get's session by it's UUID")
@Examples({"broadcast session with uuid {_s}",
        "ex2",
        "ex3"})
@SuppressWarnings("unused")
public class ExprSessionById extends SimpleExpression<Session> {

    private static final SessionManager sessionManager = SessionManager.getInstance();
    private Expression<String> uuids;

    static {
        Skript.registerExpression(ExprSessionById.class, Session.class, ExpressionType.COMBINED,
                "session[s] (with|from) [[uu]id] %strings%");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean init(Expression<?>[] exprs, int i, Kleenean kleenean, SkriptParser.ParseResult parseResult) {
        this.uuids = (Expression<String>) exprs[0];
        return true;
    }

    @Override
    protected @Nullable Session[] get(Event event) {
        return Arrays.stream(this.uuids.getAll(event))
                .map(sessionManager::getSessionById)
                .toArray(Session[]::new);
    }


    @Override
    public boolean isSingle() {
        return this.uuids.isSingle();
    }

    @Override
    public Class<? extends Session> getReturnType() {
        return Session.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "session[s] with id[s] " + this.uuids.toString(event, debug);
    }
}
