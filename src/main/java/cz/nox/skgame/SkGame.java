package cz.nox.skgame;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.core.game.GameMapManager;
import cz.nox.skgame.core.game.MiniGameManager;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.util.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class SkGame extends JavaPlugin {

    private static SkGame instance;
    private SkriptAddon addon;
    private LogUtil logUtil;

    private final File dataFolder = new File(getDataFolder(),"storage");
    private final File miniGamesDataFile = new File(dataFolder, "minigames.yml");
    private final File mapsDataFile = new File(dataFolder, "maps.yml");


    public static SkGame getInstance() {
        return instance;
    }

    public void onEnable() {
        long s = System.currentTimeMillis();

        instance = this;
        this.logUtil = new LogUtil(instance);

        ConfigurationSerialization.registerClass(CustomValue.class);

        if (getDataFolder().mkdirs()) {
            logUtil.info("Creating plugin folder");
        }

        this.addon = Skript.registerAddon(instance);

        Bukkit.getPluginManager().registerEvents(SessionManager.getInstance(), instance);

        MiniGameManager.getInstance().loadFromFile(miniGamesDataFile);
        GameMapManager.getInstance().loadFromFile(mapsDataFile);

        try {
            this.addon.loadClasses("cz.nox.skgame.skript");
        } catch (IOException e) {
            logUtil.error("Failed to load Skript classes");
        }

        logUtil.info("SkGame enabled in " + (System.currentTimeMillis() - s) + "ms");
    }

    public void onDisable() {
        long s = System.currentTimeMillis();

        MiniGameManager.getInstance().saveToFile(miniGamesDataFile);
        GameMapManager.getInstance().saveToFile(mapsDataFile);

        logUtil.info("SkGame disabled in " + (System.currentTimeMillis() - s) + "ms");
    }
}