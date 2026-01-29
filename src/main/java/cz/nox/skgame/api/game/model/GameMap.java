package cz.nox.skgame.api.game.model;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.classes.Serializer;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.variables.SerializedVariable;
import ch.njol.yggdrasil.YggdrasilSerializable;
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
                Map<String, Object> inner = new HashMap<>();
                if (innerValue instanceof YggdrasilSerializable yggSer) {
                    ClassInfo<?> ci = Classes.getExactClassInfo(innerValue.getClass());
                    assert ci != null;
                    SerializedVariable.Value serialized = Classes.serialize(innerValue);
                    assert serialized != null;
                    Map<String, Object> ser = new HashMap<>();
                    ser.put("type",serialized.type);
                    ser.put("data", serialized.data);
                    result.put(innerKey, ser);
                } else {
                    result.put(innerKey,innerValue);
                }
            });
            rawMiniGameValues.put(miniGame, result);
        });
        map.put("miniGameValues", rawMiniGameValues);
        return map;
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
                if (raw instanceof List<?> list) {
                    gameMap.setMiniGameValue(miniGameId, key, list.toArray());
                    continue;
                }
                if (raw instanceof MemorySection rawMap) {
                    Map<String, Object> data = rawMap.getValues(true);
                    String typeObj = (String) data.get("type");
                    byte[] bytes = (byte[]) data.get("data");
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
                }
                gameMap.setMiniGameValue(miniGameId, key, raw);
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
