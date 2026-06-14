package cz.nox.skgame.core.game;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.event.GameMapUnregisterEvent;
import cz.nox.skgame.api.game.model.GameMap;
import cz.nox.skgame.api.game.model.type.GameMapFilter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class GameMapManager {

    private static GameMapManager gameMapManager;
    private final Map<String, GameMap> maps = new HashMap<>();
    private final Set<String> claimedMaps = new HashSet<>();
    private GameMap lastCreatedGameMap;
    private File storageFile;

    public static synchronized GameMapManager getInstance() {
        if (gameMapManager == null) gameMapManager = new GameMapManager();
        return gameMapManager;
    }

    public void loadFromFile(File file) {
        this.storageFile = file;
        if (!file.exists()) return;
        YamlConfiguration config;
        try {
            config = YamlConfiguration.loadConfiguration(file);
        } catch (IllegalArgumentException e) {
            // Legacy maps.yml has Location stored via Bukkit ConfigurationSerialization.
            // World not loaded at onEnable() → Location.deserialize() throws "unknown world".
            // Fall back to raw SnakeYAML read that bypasses ConfigurationSerialization.
            SkGame.getInstance().getLogger().warning(
                "maps.yml contains legacy Bukkit-serialized Location values. " +
                "Falling back to raw YAML read. Re-save gamemap values to migrate. (" + e.getMessage() + ")");
            loadFromRawYaml(file);
            return;
        }
        ConfigurationSection baseSection = config.getConfigurationSection("maps");
        if (baseSection == null) return;
        for (String key : baseSection.getKeys(false)) {
            ConfigurationSection section = baseSection.getConfigurationSection(key);
            if (section == null) continue;
            GameMap gameMap = GameMap.deserialize(section.getValues(true));
            maps.put(gameMap.getId(), gameMap);
        }
        warnLegacyKeys();
    }

    @SuppressWarnings("unchecked")
    private void loadFromRawYaml(File file) {
        try (FileReader reader = new FileReader(file)) {
            org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
            Object loaded = yaml.load(reader);
            if (!(loaded instanceof Map<?, ?> root)) return;
            Object mapsRaw = root.get("maps");
            if (!(mapsRaw instanceof Map<?, ?> mapsSection)) return;
            for (Map.Entry<?, ?> entry : mapsSection.entrySet()) {
                if (!(entry.getValue() instanceof Map<?, ?> mapData)) continue;
                try {
                    GameMap gameMap = GameMap.deserialize((Map<String, Object>) mapData);
                    maps.put(gameMap.getId(), gameMap);
                } catch (Exception ex) {
                    SkGame.getInstance().getLogger().warning(
                        "Skipped map '" + entry.getKey() + "' during raw YAML load: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            SkGame.getInstance().getLogger().severe("Failed to load maps.yml via raw YAML: " + e.getMessage());
        }
        warnLegacyKeys();
        // Rewrite file in new format so next startup uses normal load path.
        if (!maps.isEmpty()) save();
    }

    private void warnLegacyKeys() {
        for (GameMap map : maps.values()) {
            for (Map.Entry<String, Map<String, Object>> mgEntry : map.getAllMiniGameValues().entrySet()) {
                for (String valueKey : mgEntry.getValue().keySet()) {
                    if (valueKey.startsWith("map;")) {
                        SkGame.getInstance().getLogger().warning(
                            "Orphaned legacy key '" + valueKey + "' for minigame '" + mgEntry.getKey() +
                            "' on map '" + map.getId() + "'. Reconfigure via /game admin. Key is ignored."
                        );
                    }
                    if (valueKey.equals("arena_region")) {
                        SkGame.getInstance().getLogger().warning(
                            "Legacy gamemap value key 'arena_region' found for minigame '" + mgEntry.getKey() +
                            "' on map '" + map.getId() + "'. Rename to 'arena' in your minigame script and reconfigure via /game admin."
                        );
                    }
                }
            }
        }
    }
    public void save() {
        if (storageFile != null) saveToFile(storageFile);
    }

    public void saveToFile(File file) {
        YamlConfiguration config = new YamlConfiguration();
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
        return maps.get(id.toLowerCase());
    }
    public GameMap[] getGameMaps() {
        return maps.values().toArray(new GameMap[0]);
    }
    public GameMap[] getGameMaps(GameMapFilter filter) {
        if (filter == null || filter == GameMapFilter.ALL) {
            return maps.values().toArray(GameMap[]::new);
        }
        final boolean claimed = (filter == GameMapFilter.CLAIMED);
        return maps.values().stream()
                .filter(map -> isMapClaimed(map.getId()) == claimed)
                .toArray(GameMap[]::new);
    }

    public GameMap registerGameMap(String id) {
        GameMap map = maps.get(id.toLowerCase());
        if (map != null) return map;
        map = new GameMap(id);
        maps.put(id.toLowerCase(),map);
        setLastCreatedGameMap(map);
        return map;
    }
    public void unregisterGameMap(String id) {
        GameMap map = maps.get(id.toLowerCase());
        if (map != null) Bukkit.getPluginManager().callEvent(new GameMapUnregisterEvent(map));
        maps.remove(id.toLowerCase());
        save();
    }
    public boolean isMapRegistered(String id) {
        return maps.containsKey(id.toLowerCase());
    }

    public boolean isMapClaimed(String id) {
        return claimedMaps.contains(id.toLowerCase());
    }
    public void addMapToClaimed(GameMap gameMap) {
        claimedMaps.add(gameMap.getId().toLowerCase());
    }
    public void removeMapFromClaimed(GameMap gameMap) {
        if (gameMap == null) return;
        claimedMaps.remove(gameMap.getId().toLowerCase());
    }

    public GameMap getLastCreatedGameMap() {
        return lastCreatedGameMap;
    }
    public void setLastCreatedGameMap(GameMap lastCreated) {
        lastCreatedGameMap = lastCreated;
    }
}
