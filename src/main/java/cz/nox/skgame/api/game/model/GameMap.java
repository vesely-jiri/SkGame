package cz.nox.skgame.api.game.model;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class GameMap implements ConfigurationSerializable {
    private String id;
    private String name;
    private Object arena;
    private Map<String, Object> info = new HashMap<>();
    private Map<String, Map<String, Object>> miniGameValues = new HashMap<>();

    public GameMap(String id) {
        this.id = id;
    }

    public GameMap(String id, String name, Object arena) {
        this.id = id;
        this.name = name;
        this.arena = arena;
    }

    public String getId() { return this.id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return this.name; }
    public void setName(String name) { this.name = name; }

    public Object getArena() { return this.arena; }
    public void setArena(Object arena) { this.arena = arena; }

    public Object getInfo(String key) { return info.get(key); }
    public void setInfo(Map<String, Object> info) { this.info = info; }
    public void addInfo(String key, Object value) { info.put(key, value); }
    public void removeInfo(String key) { info.remove(key); }

    public Object getMiniGameValue(String miniGameId, String key) {
        Map<String, Object> inner = miniGameValues.get(miniGameId);
        return inner == null ? null : inner.get(key);
    }

    public void setMiniGameValue(String miniGameId, String key, Object value) {
        Map<String, Object> map = miniGameValues.computeIfAbsent(miniGameId, k -> new HashMap<>());
        map.put(key, value);
    }

    public Map<String, Object> getMiniGameValues(String miniGameId) {
        return new HashMap<>(miniGameValues.getOrDefault(miniGameId, Map.of()));
    }

    public void setMiniGame(String miniGameId, Map<String, Object> values) {
        this.miniGameValues.put(miniGameId, values);
    }

    public Map<String, Map<String, Object>> getAllMiniGameValues() {
        return this.miniGameValues;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.id);
        map.put("name", this.name);
        map.put("arena", this.arena);
        map.put("info", this.info);
        map.put("miniGameValues", this.miniGameValues);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static GameMap deserialize(Map<String, Object> map) {
        GameMap newMap = new GameMap((String) map.get("id"));
        newMap.setName((String) map.get("name"));
        newMap.setArena(map.get("arena"));
        newMap.setInfo((Map<String, Object>) map.getOrDefault("info", new HashMap<>()));
        newMap.miniGameValues = (Map<String, Map<String, Object>>) map.getOrDefault("miniGameValues", new HashMap<>());
        return newMap;
    }
}
