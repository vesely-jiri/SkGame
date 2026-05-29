package cz.nox.skgame.skript.expressions.sessions.property;

import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Examples;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.SimplePropertyExpression;
import cz.nox.skgame.api.game.model.Session;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
@Name("Session - ID")
@Description({
        "Represents the unique identifier (ID) of a game session.",
        "Each session has its own unique string ID which can be used to identify or reference it.",
        "",
        "Useful for debugging, storing, or comparing sessions by their identifiers.",
        "",
        "Supports: GET only."
})
@Examples({
        "set {_session} to session of player",
        "broadcast id of {_session}",
        "",
        "set {_id} to id of {_session}",
        "if {_id} is \"session_001\":",
        "    broadcast \"This is the main session!\""
})
@Since("1.0.0")

public class ExprSessionId extends SimplePropertyExpression<Session, String> {

    static {
        register(ExprSessionId.class, String.class,
                "[session] id","session"
        );
    }

    @Override
    public @Nullable String convert(Session session) {
        return session.getId();
    }

    @Override
    protected String getPropertyName() {
        return "id";
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }
}
