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
    private Map<String, Map<String, Object>> gameModeValues = new HashMap<>();

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

    public Object getGameModeValue(String gameModeId, String key) {
        Map<String, Object> inner = gameModeValues.get(gameModeId);
        return inner == null ? null : inner.get(key);
    }

    public void setGameModeValue(String gameModeId, String key, Object value) {
        Map<String, Object> map = gameModeValues.computeIfAbsent(gameModeId, k -> new HashMap<>());
        map.put(key, value);
    }

    public Map<String, Object> getGameModeValues(String gameModeId) {
        return new HashMap<>(gameModeValues.getOrDefault(gameModeId, Map.of()));
    }

    public void setGameMode(String gameModeId, Map<String, Object> values) {
        this.gameModeValues.put(gameModeId, values);
    }

    public Map<String, Map<String, Object>> getAllGameModeValues() {
        return this.gameModeValues;
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", this.id);
        map.put("name", this.name);
        map.put("arena", this.arena);
        map.put("info", this.info);
        map.put("gameModeValues", this.gameModeValues);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static GameMap deserialize(Map<String, Object> map) {
        GameMap newMap = new GameMap((String) map.get("id"));
        newMap.setName((String) map.get("name"));
        newMap.setArena(map.get("arena"));
        newMap.setInfo((Map<String, Object>) map.getOrDefault("info", new HashMap<>()));
        newMap.gameModeValues = (Map<String, Map<String, Object>>) map.getOrDefault("gameModeValues", new HashMap<>());
        return newMap;
    }
}
