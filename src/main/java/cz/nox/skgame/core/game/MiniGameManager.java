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
    private final Set<String> disabledMinigames = new HashSet<>();
    private boolean shuttingDown = false;

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

    public void beginShutdown() {
        shuttingDown = true;
    }

    public void save() {
        if (storageFile == null) return;
        // Paper 1.21 calls onDisable() before setting enabled=false, so isEnabled() is unreliable
        // as a shutdown guard. Use an explicit flag set at the top of onDisable() instead.
        if (shuttingDown) return;
        SkGame plugin = SkGame.getInstance();
        if (plugin == null || !plugin.isEnabled()) return;
        saveToFile(storageFile);
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
                    SkGame.getInstance().getLogger().info("[DEBUG] loadFromFile: loaded '" + gm.getId() + "' as DISABLED");
                } else {
                    SkGame.getInstance().getLogger().info("[DEBUG] loadFromFile: loaded '" + gm.getId() + "' (enabled)");
                }
            }
        }
    }
    public void saveToFile(File file) {
        // Load existing file first: needed to preserve data for disabled-but-unloaded minigames
        // (e.g. during shutdown Skript may unload a section before onDisable() runs, removing
        // the MiniGame from miniGames. Without this, its disabled flag would be silently lost.)
        YamlConfiguration existing = YamlConfiguration.loadConfiguration(file);
        YamlConfiguration config = new YamlConfiguration();

        // Write all currently-registered minigames
        for (MiniGame gm : miniGames.values()) {
            Map<String, Object> data = new java.util.LinkedHashMap<>(gm.serialize());
            if (disabledMinigames.contains(gm.getId().toLowerCase())) data.put("disabled", true);
            config.createSection("minigames." + gm.getId().toLowerCase(), data);
        }

        // For disabled minigames that are temporarily absent from miniGames (unloaded but not
        // yet re-registered), copy their full section from the existing file so they survive.
        for (String disabledId : disabledMinigames) {
            if (miniGames.containsKey(disabledId)) continue; // already written above
            ConfigurationSection src = existing.getConfigurationSection("minigames." + disabledId);
            if (src != null) {
                for (String key : src.getKeys(true)) {
                    Object val = src.get(key);
                    if (!(val instanceof org.bukkit.configuration.ConfigurationSection)) {
                        config.set("minigames." + disabledId + "." + key, val);
                    }
                }
            }
            config.set("minigames." + disabledId + ".disabled", true);
        }

        SkGame.getInstance().getLogger().info("[DEBUG] saveToFile: miniGames=" + miniGames.keySet() + " disabledSet=" + disabledMinigames);
        if (miniGames.isEmpty()) new RuntimeException("[DEBUG] saveToFile called with EMPTY miniGames").printStackTrace();
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
        // Do NOT call save() here: unregisterMiniGame writes an incomplete list (missing the
        // just-removed minigame), which loses its disabled flag. onDisable() and postLoad()
        // are the correct persistence points — they always have the full correct state.
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
