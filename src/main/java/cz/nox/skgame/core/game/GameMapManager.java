package cz.nox.skgame.core.game;

import cz.nox.skgame.api.game.model.GameMap;

import java.util.*;

public class GameMapManager {

    private static GameMapManager gameMapManager;
    private final Map<String, GameMap> maps = new HashMap<>();
    private final HashSet<String> claimedMaps = new HashSet<>();
    private GameMap lastCreatedGameMap;

    public static synchronized GameMapManager getInstance() {
        if (gameMapManager == null) gameMapManager = new GameMapManager();
        return gameMapManager;
    }

    public void createGameMap(String id) {
        if (maps.containsKey(id)) return;
        GameMap map = new GameMap(id);
        maps.put(id,map);
        setLastCreatedGameMap(map);
    }
    public void deleteGameMap(String id) {
        maps.remove(id);
    }

    public boolean isMapClaimed(String gameMapId) {
        return claimedMaps.contains(gameMapId);
    }

    public void addMapToClaimed(GameMap gameMap) {
        claimedMaps.add(gameMap.getId());
    }
    public void removeMapFromClaimed(GameMap gameMap) {
        claimedMaps.remove(gameMap.getId());
    }

    public GameMap getLastCreatedGameMap() {
        return lastCreatedGameMap;
    }
    public void setLastCreatedGameMap(GameMap lastCreated) {
        this.lastCreatedGameMap = lastCreated;
    }
}
