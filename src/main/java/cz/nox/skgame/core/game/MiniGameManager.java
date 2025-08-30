package cz.nox.skgame.core.game;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.MiniGame;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MiniGameManager {

    private static MiniGameManager miniGameManager;
    private Map<String, MiniGame> miniGames = new HashMap<>();
    private MiniGame lastCreatedMiniGame;

    public static MiniGameManager getInstance() {
        if (miniGameManager == null) miniGameManager = new MiniGameManager();
        return miniGameManager;
    }

    public MiniGame getMiniGameById(String id) {
        return miniGames.get(id);
    }
    public MiniGame[] getAllMiniGames() {
        return miniGames.values().toArray(new MiniGame[0]);
    }

    public void loadFromFile(File file) {
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection baseSection = config.getConfigurationSection("minigames");
        if (baseSection == null) return;
        for (String key : baseSection.getKeys(false)) {
            ConfigurationSection section = baseSection.getConfigurationSection(key);
            if (section == null) continue;
            MiniGame gm = MiniGame.deserialize(section.getValues(true));
            if (gm != null) {
                miniGames.put(gm.getId(), gm);
            }
        }
    }
    public void saveToFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("minigames",null);
        for (MiniGame gm : miniGames.values()) {
            config.createSection("minigames." + gm.getId(), gm.serialize());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            SkGame.getInstance().getLogger().severe("Error while saving MiniGames: " + e.getMessage());
        }
    }

    public void registerMiniGame(String id) {
        if (miniGames.containsKey(id)) return;
        MiniGame miniGame = new MiniGame(id);
        miniGames.put(id, miniGame);
        lastCreatedMiniGame = miniGame;
    }
    public void unregisterMiniGame(String id) {
        miniGames.remove(id);
    }
    public boolean isRegistered(String id) {
        return miniGames.containsKey(id);
    }

    public MiniGame getLastCreatedMiniGame() {
        return lastCreatedMiniGame;
    }
    public void setLastCreatedMiniGame(MiniGame miniGame) {
        this.lastCreatedMiniGame = miniGame;
    }
}
