package cz.nox.skgame.skript;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.registrations.Classes;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.GameMode;
import cz.nox.skgame.api.game.model.Session;

@SuppressWarnings("unused")
public class Types {
    static {
        Classes.registerClass(new ClassInfo<>(Session.class,"session")
                .user("session")
                .name("Session")
                .description("Represents a game session")
                .defaultExpression(new EventValueExpression<>(Session.class))
                .since("1.0.0")
                .parser(new Parser<>() {
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

        Classes.registerClass(new ClassInfo<>(GameMap.class, "gamemap")
                .user("gamemap")
                .name("GameMap")
                .description("Represents map of SkGame addon")
                .defaultExpression(new EventValueExpression<>(GameMap.class))
                .since("1.0.0")
                .parser(new Parser<>() {
                    @Override
                    public boolean canParse(ParseContext context) {
                        return false;
                    }
                    @Override
                    public String toString(GameMap gameMap, int i) {
                        return "gamemap with id " + gameMap.getId();
                    }
                    @Override
                    public String toVariableNameString(GameMap gameMap) {
                        return "gamemap:" + gameMap.getId();
                    }
                })
        );

        Classes.registerClass(new ClassInfo<>(GameMode.class, "gamemode")
                .user("gamemode")
                .name("GameMode")
                .description("Represents gamemode of SkGame addon")
                .defaultExpression(new EventValueExpression<>(GameMode.class))
                .since("1.0.0")
                .parser(new Parser<>() {
                    @Override
                    public boolean canParse(ParseContext context) {
                        return false;
                    }
                    @Override
                    public String toString(GameMode gameMode, int i) {
                        return "gamemode with id " + gameMode.getId();
                    }
                    @Override
                    public String toVariableNameString(GameMode gameMode) {
                        return "gamemode:" + gameMode.getId();
                    }
                })
        );
    }
}
