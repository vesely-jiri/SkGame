package cz.nox.skgame.skript;

import ch.njol.skript.classes.*;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.yggdrasil.Fields;
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.CustomValuePlurality;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.core.game.GameMapManager;
import cz.nox.skgame.core.game.MiniGameManager;
import cz.nox.skgame.core.game.SessionManager;
import org.jetbrains.annotations.Nullable;

import java.io.NotSerializableException;
import java.io.StreamCorruptedException;

@SuppressWarnings("unused")
public class Types {
    private static final SessionManager sessionManager = SessionManager.getInstance();
    private static final GameMapManager gameMapManager = GameMapManager.getInstance();
    private static final MiniGameManager miniGameManager = MiniGameManager.getInstance();

    static {
        Classes.registerClass(new ClassInfo<>(Session.class, "session")
                .user("session")
                .name("Session")
                .description(
                        "Represents a game session.",
                        "Sessions are not persistent against server restart.",
                        "Can store default values as session properties like host,players,spectators, etc.",
                        "Can store custom values defined by scripter, that (are/are not) persistent against MiniGame restarts."
                )
                .examples(
                        "set {_session} to session with id \"my_custom_unique_id\"",
                        "set {_new} to new session with id \"my_new_custom_unique_id\""
                )
                .defaultExpression(new EventValueExpression<>(Session.class))
                .since("1.0.0")
                .parser(new Parser<Session>() {
                    @Override
                    public boolean canParse(ParseContext context) {
                        return false;
                    }

                    @Override
                    public String toString(Session session, int flags) {
                        return "session with id '" + session.getId() + "'";
                    }

                    @Override
                    public String toVariableNameString(Session session) {
                        return String.format("session:%s", session.getId());
                    }
                })
                .serializer(new Serializer<>() {
                    @Override
                    public Fields serialize(Session session) {
                        Fields fields = new Fields();
                        fields.putObject("id", session.getId());
                        return fields;
                    }

                    @Override
                    public void deserialize(Session o, Fields f) {
                        assert false;
                    }

                    @Override
                    protected Session deserialize(Fields fields) throws StreamCorruptedException {
                        String id = fields.getObject("sessionId", String.class);
                        Session session = SessionManager.getInstance().getSessionById(id);
                        if (session == null) throw new StreamCorruptedException("Unknown session ID");
                        return session;
                    }

                    @Override
                    public <E extends Session> @Nullable E newInstance(Class<E> c) {
                        return null;
                    }

                    @Override
                    public boolean mustSyncDeserialization() {
                        return true;
                    }

                    @Override
                    protected boolean canBeInstantiated() {
                        return false;
                    }
                })
                .changer(new Changer<>() {
                    @Override
                    public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
                        if (mode == ChangeMode.DELETE) {
                            return CollectionUtils.array();
                        }
                        return null;
                    }

                    @Override
                    public void change(Session[] sessions, Object @Nullable [] objects, ChangeMode mode) {
                        if (mode == ChangeMode.DELETE) {
                            for (Session session : sessions) {
                                sessionManager.deleteSession(session.getId());
                            }
                        }
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
                        return "gamemap with id '" + gameMap.getId() + "'";
                    }
                    @Override
                    public String toVariableNameString(GameMap gameMap) {
                        return "gamemap:" + gameMap.getId();
                    }
                })
                .serializer(new Serializer<GameMap>() {
                    @Override
                    public Fields serialize(GameMap o) throws NotSerializableException {
                        Fields fields = new Fields();
                        fields.putObject("id", o.getId());
                        return fields;
                    }

                    @Override
                    public void deserialize(GameMap o, Fields f) throws StreamCorruptedException, NotSerializableException {
                        assert false;
                    }

                    @Override
                    protected GameMap deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                        String id = fields.getObject("id", String.class);
                        assert id != null;
                        GameMap gm = gameMapManager.getGameMapById(id);
                        if (gm == null) throw new StreamCorruptedException("Unknown GameMap ID");
                        return gm;
                    }

                    @Override
                    public boolean mustSyncDeserialization() {
                        return true;
                    }

                    @Override
                    protected boolean canBeInstantiated() {
                        return false;
                    }
                })
                .changer(new Changer<GameMap>() {
                    @Override
                    public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
                        if (mode == ChangeMode.DELETE) {
                            return CollectionUtils.array();
                        }
                        return null;
                    }

                    @Override
                    public void change(GameMap[] gameMaps, Object @Nullable [] delta, ChangeMode mode) {
                        if (mode == ChangeMode.DELETE) {
                            for (GameMap map : gameMaps) {
                                gameMapManager.unregisterGameMap(map.getId());
                            }
                        }
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
                        return "minigame with id '" + miniGame.getId() + "'";
                    }
                    @Override
                    public String toVariableNameString(MiniGame miniGame) {
                        return "minigame:" + miniGame.getId();
                    }
                })
                .serializer(new Serializer<MiniGame>() {
                    @Override
                    public Fields serialize(MiniGame o) {
                        Fields fields = new Fields();
                        fields.putObject("id", o.getId());
                        return fields;
                    }

                    @Override
                    public void deserialize(MiniGame o, Fields f) throws StreamCorruptedException, NotSerializableException {
                        assert false;
                    }

                    @Override
                    protected MiniGame deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                        String id = fields.getObject("id", String.class);
                        assert id != null;
                        MiniGame mg = miniGameManager.getMiniGameById(id);
                        if (mg == null) throw new StreamCorruptedException("Unknown MiniGame ID");
                        return mg;
                    }

                    @Override
                    public boolean mustSyncDeserialization() {
                        return true;
                    }

                    @Override
                    protected boolean canBeInstantiated() {
                        return false;
                    }
                })
                .changer(new Changer<MiniGame>() {
                    @Override
                    public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
                        if (mode == ChangeMode.DELETE) {
                            return CollectionUtils.array();
                        }
                        return null;
                    }

                    @Override
                    public void change(MiniGame[] miniGames, Object @Nullable [] delta, ChangeMode mode) {
                        if (mode == ChangeMode.DELETE) {
                            for (MiniGame mg : miniGames) {
                                miniGameManager.unregisterMiniGame(mg.getId());
                            }
                        }
                    }
                })
        );

        Classes.registerClass(new EnumClassInfo<>(SessionState.class,"sessionstate","session states", new SimpleLiteral<>(SessionState.STOPPED, true))
                .user("session ?states?")
                .name("Session State")
                .description("Represents states of session")
                .since("1.0.0")
                .parser(new Parser<>() {

                    @Override
                    public SessionState parse(String input, ParseContext context) {
                        try {
                            return SessionState.valueOf(input.trim().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    }

                    @Override
                    public String toString(SessionState sessionState, int i) {
                        return sessionState.name();
                    }

                    @Override
                    public String toVariableNameString(SessionState sessionState) {
                        return "sessionState:" + sessionState.toString();
                    }
                })
        );

        Classes.registerClass(new EnumClassInfo<>(CustomValuePlurality.class, "customvalueplurality", "value plurality", new SimpleLiteral<>(CustomValuePlurality.SINGLE, true))
                .user("value ?plurality?")
                .name("Custom value plurality")
                .description("Represents plurality of custom value. Defaults to single/singular")
                .since("1.0.0")
                .parser(new Parser<>() {

                    @Override
                    public @Nullable CustomValuePlurality parse(String s, ParseContext context) {
                        try {
                            return CustomValuePlurality.valueOf(s.trim().toUpperCase());
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    }

                    @Override
                    public String toString(CustomValuePlurality plur, int flags) {
                        return plur.name();
                    }

                    @Override
                    public String toVariableNameString(CustomValuePlurality plur) {
                        return "valuePlurality:" + plur.toString();
                    }
                })
        );

        Classes.registerClass(new ClassInfo<>(CustomValue.class, "customvalue")
                .user("customValue")
                .name("Custom value")
                .description("Represents custom value object of a minigame value")
                .since("1.0.0")
                .parser(new Parser<>() {

                    @Override
                    public boolean canParse(ParseContext context) {
                        return false;
                    }

                    @Override
                    public String toString(CustomValue o, int flags) {
                        return "a custom value";
                    }

                    @Override
                    public String toVariableNameString(CustomValue value) {
                        return "customValue:" + value.toString();
                    }
                })
                .serializer(new Serializer<CustomValue>() {
                    @Override
                    public Fields serialize(CustomValue o) throws NotSerializableException {
                        Fields fields = new Fields();
                        fields.putObject("name", o.getName());
                        fields.putObject("type", o.getType());
                        fields.putObject("defaultValue", o.getDefaultValue());
                        fields.putObject("description", o.getDescription());
                        fields.putObject("plurality", o.getPlurality());
                        return fields;
                    }

                    @Override
                    public CustomValue deserialize(Fields fields) throws StreamCorruptedException, NotSerializableException {
                        CustomValue v = new CustomValue();
                        v.setName(fields.getObject("name", String.class));
                        v.setType(fields.getObject("type", ClassInfo.class));
                        v.setDefaultValue(fields.getObject("defaultValue", Object.class));
                        v.setDescription(fields.getObject("description", String.class));
                        v.setPlurality(fields.getObject("plurality", CustomValuePlurality.class));
                        return v;
                    }

                    @Override
                    public void deserialize(CustomValue o, Fields f) throws StreamCorruptedException, NotSerializableException {
                        assert false;
                    }

                    @Override
                    public boolean mustSyncDeserialization() {
                        return true;
                    }

                    @Override
                    protected boolean canBeInstantiated() {
                        return false;
                    }
                })
                .defaultExpression(new EventValueExpression<>(CustomValue.class))
        );
    }
}
