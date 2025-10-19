package cz.nox.skgame.api.game.model;

import ch.njol.skript.lang.util.common.AnyNamed;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class GameMap implements ConfigurationSerializable, AnyNamed {
    private String id;
    private String name;
    private Map<String, Object> values = new HashMap<>();
    /**
     * Map< MiniGameId , Map< Key , Object> >
     */
    private Map<String, Map<String, Object>> miniGameValues = new HashMap<>();

    public GameMap(String id) {
        this.id = id;
    }

    public GameMap(String id, String name) {
        this.id = id;
        this.name = name;
    }

    @Override
    public @UnknownNullability String name() {
        return this.name;
    }

    @Override
    public boolean supportsNameChange() {
        return true;
    }

    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public Object getInfo(String key) {
        return this.values.get(key);
    }
    public void setInfo(Map<String, Object> values) {
        this.values = values;
    }
    public void addInfo(String key, Object value) {
        this.values.put(key, value);
    }
    public void removeInfo(String key) {
        this.values.remove(key);
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

    public Map<String, Object> getMiniGameValues(String miniGameId) {
        return new HashMap<>(miniGameValues.getOrDefault(miniGameId, Map.of()));
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
        map.put("name", this.name);
        map.put("values", this.values);
        map.put("miniGameValues", this.miniGameValues);
        return map;
    }
    public static GameMap deserialize(Map<String, Object> map) {
        if (map == null) return null;
        String id = (String) map.get("id");
        String name = (String) map.get("name");

        GameMap newMap = new GameMap(id, name);

        Object rawInfo = map.get("values");
        if (rawInfo instanceof Map<?, ?> rawInfoMap) {
            Map<String, Object> info = new HashMap<>();
            rawInfoMap.forEach((k, v) -> info.put(String.valueOf(k), v));
            newMap.setInfo(info);
        }

        Object rawMiniGameValues = map.get("miniGameValues");
        if (rawMiniGameValues instanceof Map<?, ?> outerMap) {
            for (Map.Entry<?, ?> entry : outerMap.entrySet()) {
                String miniGameId = String.valueOf(entry.getKey());
                Object inner = entry.getValue();
                if (inner instanceof Map<?, ?> innerMap) {
                    Map<String, Object> innerValues = new HashMap<>();
                    innerMap.forEach((k, v) -> innerValues.put(String.valueOf(k), v));
                    newMap.setMiniGameValues(miniGameId, innerValues);
                }
            }
        }

        return newMap;
    }
}

