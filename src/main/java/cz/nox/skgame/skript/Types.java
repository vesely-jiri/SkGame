package cz.nox.skgame.skript;

import ch.njol.skript.classes.*;
import ch.njol.skript.expressions.base.EventValueExpression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.util.SimpleLiteral;
import ch.njol.skript.registrations.Classes;
import org.skriptlang.skript.lang.converter.Converters;
import ch.njol.util.coll.CollectionUtils;
import ch.njol.yggdrasil.Fields;
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.MinigameTag;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.SessionVisibility;
import cz.nox.skgame.api.game.model.type.CancellableEventType;
import cz.nox.skgame.api.game.model.type.CustomValuePlurality;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.game.model.type.TeamAssignmentMode;
import cz.nox.skgame.api.region.Region;
import cz.nox.skgame.api.statistics.GameResult;
import cz.nox.skgame.core.game.GameMapManager;
import cz.nox.skgame.core.game.MiniGameManager;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.region.CuboidRegion;
import cz.nox.skgame.core.region.Zone;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.io.NotSerializableException;
import java.io.StreamCorruptedException;
import java.util.Arrays;

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
                        String id = fields.getObject("id", String.class);
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
                    public @Nullable Class<?>[] acceptChange(ChangeMode mode) {
                        if (mode == ChangeMode.DELETE) {
                            return CollectionUtils.array();
                        }
                        return null;
                    }

                    @Override
                    public void change(GameMap[] gameMaps, @Nullable Object[] delta, ChangeMode mode) {
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
                    public @Nullable Class<?>[] acceptChange(ChangeMode mode) {
                        if (mode == ChangeMode.DELETE) {
                            return CollectionUtils.array();
                        }
                        return null;
                    }

                    @Override
                    public void change(MiniGame[] miniGames, @Nullable Object[] delta, ChangeMode mode) {
                        if (mode == ChangeMode.DELETE) {
                            for (MiniGame mg : miniGames) {
                                miniGameManager.unregisterMiniGame(mg.getId());
                            }
                        }
                    }
                })
        );

        Classes.registerClass(new EnumClassInfo<>(SessionState.class,"sessionstate","session states", new SimpleLiteral<>(SessionState.LOBBY, true))
                .user("session ?states?")
                .name("Session State")
                .description("Represents states of session")
                .since("1.0.0")
        );

        Classes.registerClass(new EnumClassInfo<>(SessionVisibility.class, "sessionvisibility", "session visibilities", new SimpleLiteral<>(SessionVisibility.PUBLIC, true))
                .user("session ?visibilit(y|ies)")
                .name("Session Visibility")
                .description("Represents the visibility of a session. PUBLIC (default) or PRIVATE.")
                .since("1.0.0")
        );

        Classes.registerClass(new EnumClassInfo<>(CustomValuePlurality.class, "customvalueplurality", "value plurality", new SimpleLiteral<>(CustomValuePlurality.SINGLE, true))
                .user("value ?plurality?")
                .name("Custom value plurality")
                .description("Represents plurality of custom value. Defaults to single/singular")
                .since("1.0.0")
        );

        Classes.registerClass(new EnumClassInfo<>(MinigameTag.class, "minigametag", "minigame tags",
                new SimpleLiteral<>(MinigameTag.PVP, true))
                .user("minigame ?tags?")
                .name("Minigame Tag")
                .description("Represents a category tag for a minigame (pvp, pve, ffa, team, building, puzzle, race).")
                .since("1.0.0")
        );

        // ClassInfo (not EnumClassInfo) so we can call .parser() without hitting
        // EnumClassInfo's pre-set parser and the "assert this.parser == null" in Skript 2.13.0.
        Classes.registerClass(new ClassInfo<>(TeamAssignmentMode.class, "teamassignmentmode")
                .user("team ?assignment ?modes?")
                .name("Team Assignment Mode")
                .description("How players are assigned to teams: auto (framework assigns), self-select (players choose), both (players choose + unpicked auto-balanced).")
                .since("1.0.0")
                .serializer(new EnumSerializer<>(TeamAssignmentMode.class))
                .parser(new Parser<>() {
                    @Override
                    public @Nullable TeamAssignmentMode parse(String input, ParseContext context) {
                        return switch (input.trim().toLowerCase()) {
                            case "auto"        -> TeamAssignmentMode.AUTO;
                            case "self-select" -> TeamAssignmentMode.SELF_SELECT;
                            case "both"        -> TeamAssignmentMode.BOTH;
                            default -> {
                                try { yield TeamAssignmentMode.valueOf(input.trim().toUpperCase()); }
                                catch (IllegalArgumentException e) { yield null; }
                            }
                        };
                    }
                    @Override
                    public String toString(TeamAssignmentMode mode, int flags) {
                        return mode.name().toLowerCase().replace('_', '-');
                    }
                    @Override
                    public String toVariableNameString(TeamAssignmentMode mode) {
                        return "teamAssignmentMode:" + mode.name();
                    }
                })
        );

        Classes.registerClass(new ClassInfo<>(CancellableEventType.class, "cancellableeventtype")
                .user("cancellable ?event ?types?")
                .name("Cancellable Event Type")
                .description("Represents a game event type that can be automatically cancelled for all players in a session. Used with the 'cancel events:' entry in register minigame blocks.")
                .since("1.0.0")
                .serializer(new EnumSerializer<>(CancellableEventType.class))
                .parser(new Parser<>() {
                    @Override
                    public @Nullable CancellableEventType parse(String input, ParseContext context) {
                        try {
                            return CancellableEventType.valueOf(input.trim().toUpperCase().replace('-', '_'));
                        } catch (IllegalArgumentException e) {
                            return null;
                        }
                    }
                    @Override
                    public String toString(CancellableEventType type, int flags) {
                        return type.skriptName();
                    }
                    @Override
                    public String toVariableNameString(CancellableEventType type) {
                        return "cancellableEventType:" + type.name();
                    }
                })
        );

        Classes.registerClass(new ClassInfo<>(Region.class, "skarena")
                .user("arenas?")
                .name("Arena")
                .description(
                        "Represents a 3D region in the world used as a game arena.",
                        "Only CuboidRegion (defined via admin wand) is supported.",
                        "Can be stored in Skript variables and as a GameMap property."
                )
                .examples(
                        "set arena of {_map} to cuboid region from {_pos1} to {_pos2}"
                )
                .since("1.0.0")
                .parser(new Parser<Region>() {
                    @Override
                    public boolean canParse(ParseContext context) {
                        return false;
                    }
                    @Override
                    public String toString(Region region, int flags) {
                        if (region instanceof CuboidRegion c) {
                            return "cuboid region in " + c.getWorld().getName()
                                    + " from " + locStr(c.getMin()) + " to " + locStr(c.getMax());
                        }
                        return "arena";
                    }
                    @Override
                    public String toVariableNameString(Region region) {
                        if (region instanceof CuboidRegion c) {
                            String w = c.getWorld() != null ? c.getWorld().getName() : "?";
                            return "arena:cuboid:" + w + ":" + c.getMinX() + "," + c.getMinY() + "," + c.getMinZ()
                                    + "-" + c.getMaxX() + "," + c.getMaxY() + "," + c.getMaxZ();
                        }
                        return "arena:unknown:" + System.identityHashCode(region);
                    }
                    private String locStr(Location l) {
                        return l.getBlockX() + "," + l.getBlockY() + "," + l.getBlockZ();
                    }
                })
                .serializer(new Serializer<>() {
                    @Override
                    public Fields serialize(Region region) throws NotSerializableException {
                        Fields fields = new Fields();
                        if (region instanceof CuboidRegion c) {
                            fields.putObject("type", "cuboid");
                            fields.putObject("world", c.getWorld().getName());
                            fields.putPrimitive("minX", c.getMinX());
                            fields.putPrimitive("minY", c.getMinY());
                            fields.putPrimitive("minZ", c.getMinZ());
                            fields.putPrimitive("maxX", c.getMaxX());
                            fields.putPrimitive("maxY", c.getMaxY());
                            fields.putPrimitive("maxZ", c.getMaxZ());
                        } else {
                            throw new NotSerializableException("Unsupported Region type: " + region.getClass().getName());
                        }
                        return fields;
                    }

                    @Override
                    public void deserialize(Region o, Fields f) {
                        assert false;
                    }

                    @Override
                    protected Region deserialize(Fields fields) throws StreamCorruptedException {
                        String type = fields.getObject("type", String.class);
                        if ("cuboid".equals(type)) {
                            String worldName = fields.getObject("world", String.class);
                            World world = Bukkit.getWorld(worldName != null ? worldName : "");
                            if (world == null) throw new StreamCorruptedException("Unknown world: " + worldName);
                            int minX = fields.getPrimitive("minX", int.class);
                            int minY = fields.getPrimitive("minY", int.class);
                            int minZ = fields.getPrimitive("minZ", int.class);
                            int maxX = fields.getPrimitive("maxX", int.class);
                            int maxY = fields.getPrimitive("maxY", int.class);
                            int maxZ = fields.getPrimitive("maxZ", int.class);
                            return new CuboidRegion(
                                    new Location(world, minX, minY, minZ),
                                    new Location(world, maxX, maxY, maxZ)
                            );
                        }
                        throw new StreamCorruptedException("Unknown arena type: " + type);
                    }

                    @Override
                    public <E extends Region> @Nullable E newInstance(Class<E> c) {
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
        );

        Classes.registerClass(new ClassInfo<>(Zone.class, "skzone")
                .user("zones?")
                .name("Zone")
                .description(
                        "A named sub-region within a game arena for zone-based checks (e.g. flag zones, spawn pads).",
                        "Geometrically identical to arena — set via the admin wand.",
                        "Use 'type: a zone' in gamemap value definitions."
                )
                .since("1.0.0")
                .parser(new Parser<Zone>() {
                    @Override
                    public boolean canParse(ParseContext context) {
                        return false;
                    }
                    @Override
                    public String toString(Zone z, int flags) {
                        return "zone in " + (z.getWorld() != null ? z.getWorld().getName() : "?");
                    }
                    @Override
                    public String toVariableNameString(Zone z) {
                        String w = z.getWorld() != null ? z.getWorld().getName() : "?";
                        return "zone:cuboid:" + w + ":" + z.getMinX() + "," + z.getMinY() + "," + z.getMinZ()
                                + "-" + z.getMaxX() + "," + z.getMaxY() + "," + z.getMaxZ();
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
                        return "customValue:" + (value.getName() != null ? value.getName() : "?");
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
                        if (o.hasAllowedValues()) {
                            fields.putObject("allowedValues", o.getAllowedValues().toArray(new String[0]));
                        }
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
                        String[] av = fields.getObject("allowedValues", String[].class);
                        if (av != null) v.setAllowedValues(Arrays.asList(av));
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

        Classes.registerClass(new ClassInfo<>(GameResult.class, "gameresult")
                .user("game ?results?")
                .name("Game Result")
                .description(
                        "Represents a single completed game result from the database.",
                        "Access fields with: id, minigame id, gamemap id, start time, end time, reason, winner."
                )
                .since("1.0.0")
                .parser(new Parser<GameResult>() {
                    @Override
                    public boolean canParse(ParseContext context) {
                        return false;
                    }
                    @Override
                    public String toString(GameResult r, int flags) {
                        return "game result #" + r.id() + " (" + r.minigameId() + ")";
                    }
                    @Override
                    public String toVariableNameString(GameResult r) {
                        return "gameresult:" + r.id();
                    }
                })
        );

        // Converters so that Object-typed variables ({_session}, {_mg}, {_map}) are accepted
        // wherever %session% / %minigame% / %gamemap% patterns are expected.
        Converters.registerConverter(Object.class, Session.class,
                obj -> obj instanceof Session s ? s : null);
        Converters.registerConverter(Object.class, MiniGame.class,
                obj -> obj instanceof MiniGame mg ? mg : null);
    }
}
