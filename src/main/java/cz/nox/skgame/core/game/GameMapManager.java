package cz.nox.skgame.core.game;

import cz.nox.skgame.api.game.model.GameMap;

import java.util.HashMap;
import java.util.Map;

public class GameMapManager {

    private static GameMapManager gameMapManager;
    private final Map<String, GameMap> maps = new HashMap<>();
    private GameMap lastCreatedGameMap;

    public static synchronized GameMapManager getInstance() {
        if (gameMapManager == null) gameMapManager = new GameMapManager();
        return gameMapManager;
    }

    public void createGameMap(String id) {
        if (maps.containsKey(id)) return;
        GameMap map = new GameMap(id);
        maps.put(id,map);
        lastCreatedGameMap = map;
    }
}
