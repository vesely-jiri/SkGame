package cz.nox.skgame.skript;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import cz.nox.skgame.api.game.model.Session;

public class Types {
    static {
        Classes.registerClass(new ClassInfo<>(Session.class,"session")
                .user("session")
                .name("Session")
                .description("Represents a game session")
                .defaultExpression(new EventValueExpression<>(Session.class))
                .since("1.0.0")
                .parser(new Parser<Session>() {

                    @Override
                    public boolean canParse(ParseContext context) {
                        return false;
                    }

                    @Override
                    public String toString(Session session, int i) {
                        return "session with id " + session.getId();
                    }

                    @Override
                    public String toVariableNameString(Session session) {
                        return "session:" + session.getId();
                    }
                })

        );
    }
}
