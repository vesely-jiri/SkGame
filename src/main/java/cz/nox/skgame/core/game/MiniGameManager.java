package cz.nox.skgame.core.game;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.event.MiniGameUnregisterEvent;
import cz.nox.skgame.api.game.model.MiniGame;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MiniGameManager {

    private static MiniGameManager miniGameManager;
    private Map<String, MiniGame> miniGames = new HashMap<>();
    private MiniGame lastCreatedMiniGame;
    private File storageFile;
    /** Runtime-only disabled set — NOT persisted across restarts. */
    private final Set<String> disabledMinigames = new HashSet<>();

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

    public boolean isMinigameDisabled(String id) { return disabledMinigames.contains(id.toLowerCase()); }
    public void disableMinigame(String id) { disabledMinigames.add(id.toLowerCase()); }
    public void enableMinigame(String id) { disabledMinigames.remove(id.toLowerCase()); }

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
                if (Boolean.TRUE.equals(section.get("disabled"))) {
                    disabledMinigames.add(gm.getId().toLowerCase());
                }
            }
        }
    }
    public void saveToFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("minigames",null);
        for (MiniGame gm : miniGames.values()) {
            Map<String, Object> data = new java.util.LinkedHashMap<>(gm.serialize());
            if (disabledMinigames.contains(gm.getId().toLowerCase())) data.put("disabled", true);
            config.createSection("minigames." + gm.getId().toLowerCase(), data);
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
        MiniGame mg = miniGames.get(id.toLowerCase());
        if (mg != null) Bukkit.getPluginManager().callEvent(new MiniGameUnregisterEvent(mg));
        miniGames.remove(id.toLowerCase());
        save();
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
