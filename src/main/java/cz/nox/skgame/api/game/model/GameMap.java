package cz.nox.skgame.api.game.model;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.SerializedVariable;
import cz.nox.skgame.api.region.Region;
import cz.nox.skgame.core.region.ArenaSlot;
import cz.nox.skgame.core.region.CuboidRegion;
import cz.nox.skgame.core.region.RegionCopier;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@SuppressWarnings("unused")
public class GameMap implements ConfigurationSerializable {
    private String id;
    private Map<String, Object> values = new HashMap<>();
    // Map< MiniGameId , Map< Key , Object> >
    private Map<String, Map<String, Object>> miniGameValues = new HashMap<>();
    @Nullable
    private Region region;

    private final List<ArenaSlot> arenaSlots = new ArrayList<>();
    private int configuredSlotCount = 0;
    @Nullable private Location arenaBaseLocation;
    private int arenaPadding = 16;

    public GameMap(String id) {
        this.id = id.toLowerCase();
    }

    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id.toLowerCase();
    }


    public String[] getKeys() {
        return this.values.keySet().toArray(new String[0]);
    }
    public Object[] getValues() {
        return this.values.values().toArray();
    }
    public Object getValue(String key) {
        return this.values.get(key);
    }
    public void setValue(String key, @Nullable Object o) {
        if (o == null) {
            this.values.remove(key);
            return;
        }
        this.values.put(key,o);
    }
    public void setValues(Map<String, Object> values) {
        this.values = values;
    }
    public void removeValue(String key) {
        this.values.remove(key);
    }
    public void removeValues() {
        this.values.clear();
    }

    public String[] getMiniGameKeys(String miniGameId) {
        return this.miniGameValues.get(miniGameId).keySet().toArray(new String[0]);
    }
    public Object[] getMiniGameValues(String miniGameId) {
        return this.miniGameValues.get(miniGameId).values().toArray();
    }
    public Object getMiniGameValue(String miniGameId, String key) {
        Map<String, Object> inner = this.miniGameValues.get(miniGameId);
        return inner == null ? null : inner.get(key);
    }
    public void setMiniGameValue(String miniGameId, String key, Object value) {
        if (value == null) {
            Map<String,Object> inner = this.miniGameValues.get(miniGameId);
            if (inner != null) {
                inner.remove(key);
                if (inner.isEmpty()) {
                    this.miniGameValues.remove(miniGameId);
                }
            }
            return;
        }
        Map<String, Object> map = this.miniGameValues.computeIfAbsent(miniGameId, k -> new HashMap<>());
        map.put(key, value);
    }
    public void setMiniGameValues(String miniGameId, Map<String, Object> values) {
        if (values == null) {
            this.miniGameValues.remove(miniGameId);
        } else {
            this.miniGameValues.put(miniGameId, values);
        }
    }
    public Map<String, Map<String, Object>> getAllMiniGameValues() {
        return this.miniGameValues;
    }

    public void addMiniGameValue(String miniGameId, String key, Object value) {
        if (miniGameId == null || key == null || value == null) return;

        Map<String, Object> inner = miniGameValues.computeIfAbsent(miniGameId, k -> new HashMap<>());
        Object current = inner.get(key);

        Object[] arr;

        if (current == null) {
            // Přidáváme první hodnotu → vytvoříme array
            arr = new Object[] { value };
        } else if (current.getClass().isArray()) {
            // Už je to array → rozšíříme
            Object[] oldArr = (Object[]) current;
            arr = Arrays.copyOf(oldArr, oldArr.length + 1);
            arr[oldArr.length] = value;
        } else {
            // Je to single objekt → převedeme na array
            arr = new Object[] { current, value };
        }

        inner.put(key, arr);
    }


    public void removeMiniGameValue(String miniGameId, String key, Object value) {
        if (miniGameId == null || key == null) return;
        Map<String, Object> inner = miniGameValues.get(miniGameId);
        if (inner == null) return;
        Object current = inner.get(key);
        if (current == null) return;
        if (current.getClass().isArray()) {
            Object[] arr = (Object[]) current;
            List<Object> list = new ArrayList<>(Arrays.asList(arr));
            boolean removed = list.remove(value);
            if (!removed) return;
            if (list.isEmpty()) {
                inner.remove(key);
                if (inner.isEmpty()) miniGameValues.remove(miniGameId);
            } else {
                inner.put(key, list.toArray());
            }
        } else {
            if (current.equals(value)) {
                inner.remove(key);
                if (inner.isEmpty()) miniGameValues.remove(miniGameId);
            }
        }
    }

    @Nullable
    public Region getRegion() {
        return region;
    }
    public void setRegion(@Nullable Region region) {
        this.region = region;
    }

    public boolean hasArenaSlots() { return configuredSlotCount > 0; }
    public int getConfiguredSlotCount() { return configuredSlotCount; }
    public List<ArenaSlot> getArenaSlots() { return arenaSlots; }

    public void configureArenaSlots(int count, Location base, int padding) {
        arenaSlots.removeIf(ArenaSlot::isTemporary);
        this.arenaBaseLocation = base.clone();
        this.arenaPadding = padding;
        this.configuredSlotCount = count;

        if (region == null) return;
        Location sourceMin = region.getMin();
        Location sourceMax = region.getMax();
        if (sourceMin == null || sourceMax == null) return;
        int regionWidth = sourceMax.getBlockX() - sourceMin.getBlockX() + 1;
        int spacing = regionWidth + padding;

        while (arenaSlots.size() < count) {
            int i = arenaSlots.size();
            Location origin = new Location(base.getWorld(),
                    base.getBlockX() + (long) i * spacing, base.getBlockY(), base.getBlockZ());
            RegionCopier.copy(region, origin);
            arenaSlots.add(new ArenaSlot(origin, false));
        }
        while (arenaSlots.size() > count) {
            arenaSlots.remove(arenaSlots.size() - 1);
        }
    }

    @Nullable
    public ArenaSlot claimSlot(String sessionId) {
        for (ArenaSlot slot : arenaSlots) {
            if (slot.isFree()) {
                slot.claim(sessionId);
                return slot;
            }
        }
        // Overflow — create temporary slot
        if (region == null || arenaBaseLocation == null) return null;
        Location sourceMin = region.getMin();
        Location sourceMax = region.getMax();
        if (sourceMin == null || sourceMax == null) return null;
        int regionWidth = sourceMax.getBlockX() - sourceMin.getBlockX() + 1;
        int spacing = regionWidth + arenaPadding;
        int index = arenaSlots.size();
        Location tempOrigin = new Location(arenaBaseLocation.getWorld(),
                arenaBaseLocation.getBlockX() + (long) index * spacing,
                arenaBaseLocation.getBlockY(), arenaBaseLocation.getBlockZ());
        RegionCopier.copy(region, tempOrigin);
        ArenaSlot tempSlot = new ArenaSlot(tempOrigin, true);
        tempSlot.claim(sessionId);
        arenaSlots.add(tempSlot);
        return tempSlot;
    }

    public void releaseSlot(String sessionId) {
        for (int i = 0; i < arenaSlots.size(); i++) {
            ArenaSlot slot = arenaSlots.get(i);
            if (sessionId.equals(slot.getClaimedBySessionId())) {
                if (region != null) {
                    RegionCopier.copy(region, slot.getPasteOrigin());
                }
                slot.release();
                if (slot.isTemporary()) {
                    arenaSlots.remove(i);
                }
                return;
            }
        }
    }

    @Nullable
    public CuboidRegion getSlotRegion(ArenaSlot slot) {
        if (region == null) return null;
        Location sourceMin = region.getMin();
        Location sourceMax = region.getMax();
        if (sourceMin == null || sourceMax == null) return null;
        Location origin = slot.getPasteOrigin();
        int dx = sourceMax.getBlockX() - sourceMin.getBlockX();
        int dy = sourceMax.getBlockY() - sourceMin.getBlockY();
        int dz = sourceMax.getBlockZ() - sourceMin.getBlockZ();
        Location slotMax = new Location(origin.getWorld(),
                origin.getBlockX() + dx, origin.getBlockY() + dy, origin.getBlockZ() + dz);
        return new CuboidRegion(origin, slotMax);
    }

    public Set<String> getSupportedMiniGameIds() {
        return this.miniGameValues.keySet();
    }
    public boolean supportsMiniGame(MiniGame minigame) {
        return this.miniGameValues.containsKey(minigame.getId());
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("values", values);
        Map<String, Object> rawMiniGameValues = new HashMap<>();

        miniGameValues.forEach((miniGame, v) -> {
            Map<String, Object> result = new HashMap<>();

            v.forEach((innerKey, innerValue) -> {
                if (innerValue instanceof Object[] arr) {
                    Map<String, Object> plural = new HashMap<>();
                    plural.put("__plural", true);
                    List<Object> serializedArr = new ArrayList<>();
                    for (Object el : arr) {
                        if (el instanceof Location loc) {
                            Map<String, Object> lm = serializeLocation(loc);
                            if (!lm.isEmpty()) serializedArr.add(lm);
                        } else {
                            serializedArr.add(el);
                        }
                    }
                    plural.put("values", serializedArr);
                    result.put(innerKey, plural);
                } else if (innerValue instanceof Region region) {
                    Map<String, Object> ser = new HashMap<>(serializeRegion(region));
                    if (!ser.isEmpty()) {
                        ser.put("__region", true);
                        result.put(innerKey, ser);
                    }
                } else {
                    SerializedVariable.Value serialized = Classes.serialize(innerValue);
                    if (serialized != null) {
                        Map<String, Object> ser = new HashMap<>();
                        ser.put("type", serialized.type);
                        ser.put("data", serialized.data);
                        result.put(innerKey, ser);
                    } else if (innerValue instanceof Location loc) {
                        Map<String, Object> lm = serializeLocation(loc);
                        if (!lm.isEmpty()) result.put(innerKey, lm);
                    } else {
                        result.put(innerKey, innerValue);
                    }
                }
            });
            rawMiniGameValues.put(miniGame, result);
        });
        map.put("miniGameValues", rawMiniGameValues);
        if (region != null) {
            map.put("region", serializeRegion(region));
        }
        map.put("configuredSlotCount", configuredSlotCount);
        map.put("arenaPadding", arenaPadding);
        if (arenaBaseLocation != null && arenaBaseLocation.getWorld() != null) {
            Map<String, Object> base = new HashMap<>();
            base.put("world", arenaBaseLocation.getWorld().getName());
            base.put("x", arenaBaseLocation.getX());
            base.put("y", arenaBaseLocation.getY());
            base.put("z", arenaBaseLocation.getZ());
            map.put("arenaBase", base);
        }
        if (!arenaSlots.isEmpty()) {
            List<Map<String, Object>> slotList = new ArrayList<>();
            for (ArenaSlot slot : arenaSlots) {
                if (slot.isTemporary()) continue;
                Location o = slot.getPasteOrigin();
                if (o.getWorld() == null) continue;
                Map<String, Object> slotMap = new HashMap<>();
                slotMap.put("world", o.getWorld().getName());
                slotMap.put("x", o.getX());
                slotMap.put("y", o.getY());
                slotMap.put("z", o.getZ());
                slotList.add(slotMap);
            }
            map.put("arenaSlots", slotList);
        }
        return map;
    }

    private static Map<String, Object> serializeRegion(Region region) {
        Map<String, Object> r = new HashMap<>();
        if (region instanceof CuboidRegion cuboid) {
            r.put("type", "cuboid");
            r.put("world", cuboid.getWorld().getName());
            r.put("minX", cuboid.getMinX());
            r.put("minY", cuboid.getMinY());
            r.put("minZ", cuboid.getMinZ());
            r.put("maxX", cuboid.getMaxX());
            r.put("maxY", cuboid.getMaxY());
            r.put("maxZ", cuboid.getMaxZ());
        }
        return r;
    }

    @Nullable
    private static Region deserializeRegion(Object raw) {
        Map<String, Object> data;
        if (raw instanceof MemorySection sec) {
            data = sec.getValues(false);
        } else if (raw instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) m;
            data = cast;
        } else {
            return null;
        }
        String type = (String) data.get("type");
        if ("cuboid".equals(type)) {
            String worldName = (String) data.get("world");
            World world = Bukkit.getWorld(worldName != null ? worldName : "");
            if (world == null) return null;
            int minX = toInt(data.get("minX"));
            int minY = toInt(data.get("minY"));
            int minZ = toInt(data.get("minZ"));
            int maxX = toInt(data.get("maxX"));
            int maxY = toInt(data.get("maxY"));
            int maxZ = toInt(data.get("maxZ"));
            return new CuboidRegion(new Location(world, minX, minY, minZ), new Location(world, maxX, maxY, maxZ));
        }
        return null;
    }

    private static int toInt(Object o) {
        if (o instanceof Number n) return n.intValue();
        return 0;
    }

    private static double toDouble(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        return 0.0;
    }

    private static Map<String, Object> serializeLocation(Location loc) {
        Map<String, Object> m = new HashMap<>();
        if (loc.getWorld() == null) return m;
        m.put("__location", true);
        m.put("world", loc.getWorld().getName());
        m.put("x", loc.getX());
        m.put("y", loc.getY());
        m.put("z", loc.getZ());
        m.put("yaw", (double) loc.getYaw());
        m.put("pitch", (double) loc.getPitch());
        return m;
    }

    @Nullable
    private static Location deserializeLocation(Object raw) {
        Map<String, Object> data;
        if (raw instanceof MemorySection sec) {
            data = sec.getValues(false);
        } else if (raw instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked") Map<String, Object> cast = (Map<String, Object>) m;
            data = cast;
        } else {
            return null;
        }
        String worldName = (String) data.get("world");
        World world = Bukkit.getWorld(worldName != null ? worldName : "");
        if (world == null) return null;
        double x = toDouble(data.get("x")), y = toDouble(data.get("y")), z = toDouble(data.get("z"));
        float yaw = (float) toDouble(data.get("yaw")), pitch = (float) toDouble(data.get("pitch"));
        return new Location(world, x, y, z, yaw, pitch);
    }

    public static GameMap deserialize(Map<String, Object> map) {
        String id = (String) map.get("id");
        GameMap gameMap = new GameMap(id);
        Map<String, Object> vals = convertToMap(map.get("values"));
        gameMap.setValues(vals);
        Map<String, Object> mgVals = convertToMap(map.get("miniGameValues"));

        for (Map.Entry<String, Object> mgEntry : mgVals.entrySet()) {
            String miniGameId = mgEntry.getKey();
            Map<String, Object> innerMap = convertToMap(mgEntry.getValue());

            for (Map.Entry<String, Object> valEntry : innerMap.entrySet()) {
                String key = valEntry.getKey();
                Object raw = valEntry.getValue();
                Map<String, Object> data = null;
                if (raw instanceof MemorySection sec) data = sec.getValues(false);
                else if (raw instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked") Map<String, Object> cast = (Map<String, Object>) m;
                    data = cast;
                }
                if (data != null) {
                    if (Boolean.TRUE.equals(data.get("__region"))) {
                        Region r = deserializeRegion(raw);
                        if (r != null) {
                            gameMap.setMiniGameValue(miniGameId, key, r);
                            continue;
                        }
                    }
                    if (Boolean.TRUE.equals(data.get("__location"))) {
                        Location loc = deserializeLocation(raw);
                        if (loc != null) gameMap.setMiniGameValue(miniGameId, key, loc);
                        continue;
                    }
                    if (Boolean.TRUE.equals(data.get("__plural"))) {
                        Object rawList = data.get("values");
                        List<Object> elements = new ArrayList<>();
                        if (rawList instanceof List<?> pl) {
                            for (Object el : pl) {
                                Map<String, Object> elData = null;
                                if (el instanceof MemorySection elSec) elData = elSec.getValues(false);
                                else if (el instanceof Map<?, ?> elMap) {
                                    @SuppressWarnings("unchecked") Map<String, Object> elCast = (Map<String, Object>) elMap;
                                    elData = elCast;
                                }
                                if (elData != null && (Boolean.TRUE.equals(elData.get("__location"))
                                        || "org.bukkit.Location".equals(elData.get("==")))) {
                                    Location loc = deserializeLocation(el);
                                    if (loc != null) elements.add(loc);
                                    continue;
                                }
                                elements.add(el);
                            }
                        }
                        gameMap.setMiniGameValue(miniGameId, key, elements.toArray());
                        continue;
                    }
                    String typeObj = (String) data.get("type");
                    Object rawBytes = data.get("data");
                    byte[] bytes = rawBytes instanceof byte[] b ? b : null;
                    if (typeObj != null && bytes != null) {
                        ClassInfo<?> classInfo = Classes.getClassInfoNoError(typeObj);
                        if (classInfo != null) {
                            Serializer<?> ser = classInfo.getSerializer();
                            if (ser != null) {
                                Object deserialized = Classes.deserialize(classInfo, bytes);
                                if (deserialized != null) {
                                    gameMap.setMiniGameValue(miniGameId, key, deserialized);
                                    continue;
                                }
                            }
                        }
                    }
                    // Legacy backwards compat: old Bukkit-serialized Location (from SnakeYAML raw read)
                    if ("org.bukkit.Location".equals(data.get("=="))) {
                        Location loc = deserializeLocation(raw);
                        if (loc != null) gameMap.setMiniGameValue(miniGameId, key, loc);
                        continue;
                    }
                }
                gameMap.setMiniGameValue(miniGameId, key, raw);
            }
        }
        Object rawRegion = map.get("region");
        if (rawRegion != null) {
            gameMap.setRegion(deserializeRegion(rawRegion));
        }
        gameMap.configuredSlotCount = toInt(map.get("configuredSlotCount"));
        gameMap.arenaPadding = map.containsKey("arenaPadding") ? toInt(map.get("arenaPadding")) : 16;
        Object rawBase = map.get("arenaBase");
        if (rawBase != null) {
            Map<String, Object> baseData = convertToMap(rawBase);
            String bWorld = (String) baseData.get("world");
            World bw = Bukkit.getWorld(bWorld != null ? bWorld : "");
            if (bw != null) {
                gameMap.arenaBaseLocation = new Location(bw,
                        toDouble(baseData.get("x")), toDouble(baseData.get("y")), toDouble(baseData.get("z")));
            }
        }
        Object rawSlots = map.get("arenaSlots");
        if (rawSlots instanceof List<?> slotList) {
            for (Object slotRaw : slotList) {
                Map<String, Object> slotData = convertToMap(slotRaw);
                String sw = (String) slotData.get("world");
                World slotWorld = Bukkit.getWorld(sw != null ? sw : "");
                if (slotWorld == null) continue;
                Location origin = new Location(slotWorld,
                        toDouble(slotData.get("x")), toDouble(slotData.get("y")), toDouble(slotData.get("z")));
                gameMap.arenaSlots.add(new ArenaSlot(origin, false));
            }
        }
        return gameMap;
    }

    @SuppressWarnings("unchecked")
    private static @NotNull Map<String, Object> convertToMap(Object o) {
        if (o instanceof MemorySection sec) {
            return sec.getValues(false);
        } else if (o instanceof Map<?,?> map) {
            return (Map<String, Object>) map;
        }

        return new HashMap<>();
    }
}
