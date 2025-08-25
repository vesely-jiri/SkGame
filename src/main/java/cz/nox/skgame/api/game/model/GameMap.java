package cz.nox.skgame.api.game.model;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class GameMap {
    String id;
    String name;
    Object arena;
    HashMap<String, Object> info;
    HashMap<GameMode, Map<String, Object>> gameModeValues;

    public GameMap(String id) {
        this.id = id;
    }

    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public Object getGameModeValue(GameMode gameMode, String id) {
        Map<String, Object> inner = gameModeValues.get(gameMode);
        if (inner == null) return null;
        return inner.get(id);
    }
    public void setGameModeValue(GameMode gameMode, String key, Object value) {
        Map<String, Object> map = gameModeValues.computeIfAbsent(gameMode, k -> new HashMap<>());
        map.put(key,value);
    }
    public Map<String, Object> getGameModeValues(GameMode gameMode) {
        return new HashMap<>(gameModeValues.get(gameMode));
    }
    public void setGameMode(GameMode gameMode, HashMap<String, Object> gameModeValues) {
        this.gameModeValues.put(gameMode,gameModeValues);
    }
    public Object getInfo(String id) {
        return info.get(id);
    }
    public void setInfo(HashMap<String, Object> info) {
        this.info = info;
    }
    public void addInfo(String key, Object value) {
        info.put(key,value);
    }
    public void removeInfo(String key) {
        info.remove(key);
    }

    public Object getArena() {
        return arena;
    }
    public void setArena(Object arena) {
        this.arena = arena;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
}