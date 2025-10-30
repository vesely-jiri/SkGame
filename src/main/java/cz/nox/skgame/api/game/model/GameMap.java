package cz.nox.skgame.api.game.model;

import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class GameMap implements ConfigurationSerializable {
    private String id;
    private Map<String, Object> values = new HashMap<>();
    // Map< MiniGameId , Map< Key , Object> >
    private Map<String, Map<String, Object>> miniGameValues = new HashMap<>();

    public GameMap(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
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

    public Set<String> getSupportedMiniGameIds() {
        return this.miniGameValues.keySet();
    }
    public boolean supportsMiniGame(MiniGame minigame) {
        return this.miniGameValues.containsKey(minigame.getId());
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.id);
        map.put("values", this.values);
        map.put("miniGameValues", this.miniGameValues);
        return map;
    }
    public static GameMap deserialize(Map<String, Object> map) {
        if (map == null) return null;
        String id = (String) map.get("id");
        GameMap gameMap = new GameMap(id);
        Object rawValues = map.get("values");
        gameMap.setValues(convertSectionToMap(rawValues));
        Object rawMiniGameValues = map.get("miniGameValues");
        if (rawMiniGameValues instanceof MemorySection outerMs) {
            for (String miniGameId : outerMs.getKeys(false)) {
                Object inner = outerMs.get(miniGameId);
                gameMap.setMiniGameValues(miniGameId, convertSectionToMap(inner));
            }
        } else if (rawMiniGameValues instanceof Map<?, ?> outerMap) {
            for (Map.Entry<?, ?> entry : outerMap.entrySet()) {
                String miniGameId = String.valueOf(entry.getKey());
                gameMap.setMiniGameValues(miniGameId, convertSectionToMap(entry.getValue()));
            }
        }
        return gameMap;
    }

    private static Map<String, Object> convertSectionToMap(Object raw) {
        Map<String, Object> result = new HashMap<>();
        if (raw instanceof MemorySection ms) {
            result.putAll(ms.getValues(false));
        } else if (raw instanceof Map<?, ?> map) {
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
        }
        return result;
    }

}

