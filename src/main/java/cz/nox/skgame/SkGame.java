package cz.nox.skgame;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import cz.nox.skgame.core.game.GameMapManager;
import cz.nox.skgame.core.game.GameModeManager;
import cz.nox.skgame.util.LogUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class SkGame extends JavaPlugin {

    private static SkGame instance;
    private SkriptAddon addon;
    private LogUtil logUtil;

    private final File dataFolder = new File(getDataFolder(),"storage");
    private final File gameModesDataFile = new File(dataFolder, "gamemodes.yml");
    private final File mapsDataFile = new File(dataFolder, "maps.yml");


    public static SkGame getInstance() {
        return instance;
    }

    public void onEnable() {
        long s = System.currentTimeMillis();

        this.instance = this;
        this.logUtil = new LogUtil(instance);

        logUtil.info("SkGame enabling");

        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        this.addon = Skript.registerAddon(instance);

        GameModeManager.getInstance().loadFromFile(gameModesDataFile);
        GameMapManager.getInstance().loadFromFile(mapsDataFile);

        try {
            this.addon.loadClasses("cz.nox.skgame.skript");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logUtil.info("SkGame enabled in " + (System.currentTimeMillis() - s) + "ms");
    }

    public void onDisable() {
        long s = System.currentTimeMillis();
        logUtil.info("SkGame disabling");

        GameModeManager.getInstance().saveToFile(gameModesDataFile);
        GameMapManager.getInstance().saveToFile(mapsDataFile);

        logUtil.info("SkGame disabled in " + (System.currentTimeMillis() - s) + "ms");
    }
}