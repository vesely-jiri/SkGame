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
    private File storageFile;

    public static MiniGameManager getInstance() {
        if (miniGameManager == null) miniGameManager = new MiniGameManager();
        return miniGameManager;
    }

    public MiniGame getMiniGameById(String id) {
        return miniGames.get(id.toLowerCase());
    }
    public MiniGame[] getAllMiniGames() {
        return miniGames.values().toArray(new MiniGame[0]);
    }

    public void save() {
        if (storageFile != null) saveToFile(storageFile);
    }

    public void loadFromFile(File file) {
        this.storageFile = file;
        if (!file.exists()) return;
        YamlConfiguration config;
        try {
            config = YamlConfiguration.loadConfiguration(file);
        } catch (Exception e) {
            SkGame.getInstance().getLogger().severe(
                "minigames.yml is corrupted (" + e.getMessage() + "). Resetting. Old file renamed to minigames.yml.corrupted");
            File backup = new File(file.getParentFile(), file.getName() + ".corrupted");
            //noinspection ResultOfMethodCallIgnored
            file.renameTo(backup);
            return;
        }
        ConfigurationSection baseSection = config.getConfigurationSection("minigames");
        if (baseSection == null) return;
        for (String key : baseSection.getKeys(false)) {
            ConfigurationSection section = baseSection.getConfigurationSection(key);
            if (section == null) continue;
            MiniGame gm = MiniGame.deserialize(section.getValues(true));
            if (gm != null) {
                miniGames.put(gm.getId().toLowerCase(), gm);
            }
        }
    }
    public void saveToFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("minigames",null);
        for (MiniGame gm : miniGames.values()) {
            config.createSection("minigames." + gm.getId().toLowerCase(), gm.serialize());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            SkGame.getInstance().getLogger().severe("Error while saving MiniGames: " + e.getMessage());
        }
    }

    public MiniGame registerMiniGame(String id) {
        MiniGame mg = miniGames.get(id.toLowerCase());
        if (mg != null) return mg;
        mg = new MiniGame(id.toLowerCase());
        miniGames.put(mg.getId().toLowerCase(), mg);
        lastCreatedMiniGame = mg;
        return mg;
    }
    public void unregisterMiniGame(String id) {
        miniGames.remove(id.toLowerCase());
    }
    public boolean isRegistered(String id) {
        return miniGames.containsKey(id.toLowerCase());
    }

    public MiniGame getLastCreatedMiniGame() {
        return lastCreatedMiniGame;
    }
    public void setLastCreatedMiniGame(MiniGame miniGame) {
        this.lastCreatedMiniGame = miniGame;
    }
}
