package cz.nox.skgame;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public class SkGameAddon extends JavaPlugin {

    private static SkGameAddon instance;
    private SkriptAddon addon;

    public void onEnable() {

        long s = System.currentTimeMillis();

        instance = this;

        addon = Skript.registerAddon(instance);

        try {
            this.addon.loadClasses("cz.nox.skgame.skript");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("SkGameAddon enabled in " + System.currentTimeMillis());
    }

    public static SkGameAddon getInstance() {
        return instance;
    }
}