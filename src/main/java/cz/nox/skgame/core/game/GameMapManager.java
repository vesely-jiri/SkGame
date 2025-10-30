package cz.nox.skgame.core.game;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.type.GameMapFilter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GameMapManager {

    private static GameMapManager gameMapManager;
    private final Map<String, GameMap> maps = new HashMap<>();
    private final Set<String> claimedMaps = new HashSet<>();
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
            this.maps.put(gameMap.getId(), gameMap);
        }
    }
    public void saveToFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("maps",null);
        for (GameMap gameMap : this.maps.values()) {
            config.createSection("maps." + gameMap.getId(), gameMap.serialize());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            SkGame.getInstance().getLogger().severe("Error while saving GameMaps: " + e.getMessage());
        }
    }

    public GameMap getGameMapById(String id) {
        return this.maps.get(id);
    }
    public GameMap[] getGameMaps() {
        return this.maps.values().toArray(new GameMap[0]);
    }
    public GameMap[] getGameMaps(GameMapFilter filter) {
        if (filter == null || filter == GameMapFilter.ALL) {
            return this.maps.values().toArray(GameMap[]::new);
        }
        final boolean claimed = (filter == GameMapFilter.CLAIMED);
        return this.maps.values().stream()
                .filter(map -> isMapClaimed(map.getId()) == claimed)
                .toArray(GameMap[]::new);
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
        this.maps.remove(id);
    }
    public boolean isMapRegistered(String id) {
        return this.maps.containsKey(id);
    }

    public boolean isMapClaimed(String gameMapId) {
        return this.claimedMaps.contains(gameMapId);
    }
    public void addMapToClaimed(GameMap gameMap) {
        this.claimedMaps.add(gameMap.getId());
    }
    public void removeMapFromClaimed(GameMap gameMap) {
        this.claimedMaps.remove(gameMap.getId());
    }

    public GameMap getLastCreatedGameMap() {
        return this.lastCreatedGameMap;
    }
    public void setLastCreatedGameMap(GameMap lastCreated) {
        this.lastCreatedGameMap = lastCreated;
    }
}
