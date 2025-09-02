package cz.nox.skgame.core.game;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.GameMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
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

    public void loadFromFile(File file) {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection baseSection = config.getConfigurationSection("maps");
        if (baseSection == null) return;
        for (String key : baseSection.getKeys(false)) {
            ConfigurationSection section = baseSection.getConfigurationSection(key);
            if (section == null) continue;
            GameMap gameMap = GameMap.deserialize(section.getValues(true));
            maps.put(gameMap.getId(), gameMap);
        }
    }
    public void saveToFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("maps",null);
        for (GameMap gameMap : maps.values()) {
            config.createSection("maps." + gameMap.getId(), gameMap.serialize());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            SkGame.getInstance().getLogger().severe("Error while saving GameMaps: " + e.getMessage());
        }
    }

    public GameMap getGameMapById(String id) {
        return maps.get(id);
    }
    public GameMap[] getAllGameMaps() {
        return maps.values().toArray(new GameMap[0]);
    }

    public GameMap registerGameMap(String id) {
        GameMap map = maps.get(id);
        if (map != null) return map;
        map = new GameMap(id);
        maps.put(id,map);
        setLastCreatedGameMap(map);
        return map;
    }
    public void unregisterGameMap(String id) {
        maps.remove(id);
    }
    public boolean isMapRegistered(String id) {
        return maps.containsKey(id);
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
