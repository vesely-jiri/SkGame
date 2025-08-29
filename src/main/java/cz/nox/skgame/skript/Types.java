package cz.nox.skgame.skript;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.EnumClassInfo;
import ch.njol.skript.classes.Parser;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.registrations.Classes;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import org.jetbrains.annotations.Nullable;

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

        Classes.registerClass(new ClassInfo<>(MiniGame.class, "minigame")
                .user("minigame")
                .name("MiniGame")
                .description("Represents minigame of SkGame addon")
                .defaultExpression(new EventValueExpression<>(MiniGame.class))
                .since("1.0.0")
                .parser(new Parser<>() {
                    @Override
                    public boolean canParse(ParseContext context) {
                        return false;
                    }
                    @Override
                    public String toString(MiniGame miniGame, int i) {
                        return "minigame with id " + miniGame.getId();
                    }
                    @Override
                    public String toVariableNameString(MiniGame miniGame) {
                        return "minigame:" + miniGame.getId();
                    }
                })
        );

        Classes.registerClass(new EnumClassInfo<>(SessionState.class,"sessionstate","session states", new SimpleLiteral<>(SessionState.STOPPED, true))
                .user("session ?states?")
                .name("Session State")
                .description("Represents states of session")
                .since("1.0.0")
                .parser(new Parser<SessionState>() {

                    @Override
                    public SessionState parse(String input, ParseContext context) {
                        try {
                            return SessionState.valueOf(input.trim().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    }

                    @Override
                    public boolean canParse(ParseContext context) {
                        return true;
                    }

                    @Override
                    public String toString(SessionState sessionState, int i) {
                        return "";
                    }

                    @Override
                    public String toVariableNameString(SessionState sessionState) {
                        return "";
                    }
                })
        );
    }
}
