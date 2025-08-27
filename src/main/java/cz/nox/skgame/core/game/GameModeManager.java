package cz.nox.skgame.core.game;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.GameMode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GameModeManager {

    private static GameModeManager gameModeManager;
    private Map<String, GameMode> gameModes = new HashMap<>();
    private GameMode lastCreatedGameMode;

    public static GameModeManager getInstance() {
        if (gameModeManager == null) gameModeManager = new GameModeManager();
        return gameModeManager;
    }

    public GameMode getGameModeById(String id) {
        return gameModes.get(id);
    }

    public void loadFromFile(File file) {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection baseSection = config.getConfigurationSection("gamemodes");
        if (baseSection == null) return;
        for (String key : baseSection.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection("gamemodes." + key);
            if (section == null) continue;
            Map<String, Object> gmData = section.getValues(true);
            GameMode gm = GameMode.deserialize(gmData);
            gameModes.put(gm.getId(),gm);
        }
    }
    public void saveToFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("gamemodes",null);
        for (GameMode gm : gameModes.values()) {
            config.createSection("gamemodes." + gm.getId(), gm.serialize());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            SkGame.getInstance().getLogger().severe("Error while saving GameModes: " + e.getMessage());
        }
    }

    public void registerGameMode(String id) {
        if (gameModes.containsKey(id)) return;
        GameMode gameMode = new GameMode(id);
        gameModes.put(id,gameMode);
        lastCreatedGameMode = gameMode;
    }
    public void unregisterGameMode(String id) {
        gameModes.remove(id);
    }
    public boolean isRegistered(String id) {
        return gameModes.containsKey(id);
    }

    public GameMode getLastCreatedGameMode() {
        return lastCreatedGameMode;
    }
    public void setLastCreatedGameMode(GameMode gameMode) {
        this.lastCreatedGameMode = gameMode;
    }
}
