package cz.nox.skgame;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import cz.nox.skgame.util.LogUtil;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class SkGameAddon extends JavaPlugin {

    private static SkGameAddon instance;
    private SkriptAddon addon;
    private LogUtil logUtil;

    public void onEnable() {

        long s = System.currentTimeMillis();

        logUtil.info("SkGameAddon enabling");

        instance = this;

        logUtil = new LogUtil(instance);

        addon = Skript.registerAddon(instance);

        try {
            this.addon.loadClasses("cz.nox.skgame.skript");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logUtil.info("SkGameAddon enabled in " + (System.currentTimeMillis() - s) + "ms");
    }

    public static SkGameAddon getInstance() {
        return instance;
    }
}