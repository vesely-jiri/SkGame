package cz.nox.skgame;

import ch.njol.skript.Skript;
import ch.njol.skript.ScriptLoader;
import ch.njol.skript.SkriptAddon;
import ch.njol.util.OpenCloseable;
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.core.game.GameMapManager;
import cz.nox.skgame.core.game.MiniGameManager;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.region.RegionFactory;
import cz.nox.skgame.core.region.SkBeeBoundRegion;
import cz.nox.skgame.core.region.WorldGuardRegion;
import cz.nox.skgame.util.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class SkGame extends JavaPlugin {

    private static SkGame instance;
    private SkriptAddon addon;
    private LogUtil logUtil;

    private final File dataFolder = new File(getDataFolder(), "storage");
    private final File miniGamesDataFile = new File(dataFolder, "minigames.yml");
    private final File mapsDataFile = new File(dataFolder, "maps.yml");

    @Nullable
    private Location lobbySpawn;

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

        saveDefaultConfig();
        loadLobbySpawn();

        this.addon = Skript.registerAddon(instance);
        registerRegionAdapters();
        copyVanillaScripts();

        Bukkit.getPluginManager().registerEvents(SessionManager.getInstance(), instance);

        try {
            this.addon.loadClasses("cz.nox.skgame.skript");
        } catch (IOException e) {
            logUtil.error("Failed to load Skript classes");
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            MiniGameManager.getInstance().loadFromFile(miniGamesDataFile);
            GameMapManager.getInstance().loadFromFile(mapsDataFile);
        }, 1L);

        logUtil.info("SkGame enabled in " + (System.currentTimeMillis() - s) + "ms");
    }

    public void onDisable() {
        long s = System.currentTimeMillis();

        MiniGameManager.getInstance().saveToFile(miniGamesDataFile);
        GameMapManager.getInstance().saveToFile(mapsDataFile);

        logUtil.info("SkGame disabled in " + (System.currentTimeMillis() - s) + "ms");
    }

    @Nullable
    public Location getLobbySpawn() {
        return lobbySpawn;
    }

    public void setLobbySpawn(@Nullable Location location) {
        this.lobbySpawn = location;
        if (location == null || location.getWorld() == null) {
            getConfig().set("lobby.world", null);
        } else {
            getConfig().set("lobby.world", location.getWorld().getName());
            getConfig().set("lobby.x", location.getX());
            getConfig().set("lobby.y", location.getY());
            getConfig().set("lobby.z", location.getZ());
            getConfig().set("lobby.yaw", location.getYaw());
            getConfig().set("lobby.pitch", location.getPitch());
        }
        saveConfig();
    }

    public GameMode getDefaultGameMode() {
        String raw = getConfig().getString("defaults.gamemode", "adventure");
        try {
            return GameMode.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GameMode.ADVENTURE;
        }
    }

    private void registerRegionAdapters() {
        if (getConfig().getBoolean("regions.enable-skbee-adapter", true)
                && Bukkit.getPluginManager().getPlugin("SkBee") != null) {
            if (SkBeeBoundRegion.init()) {
                @SuppressWarnings("unchecked")
                Class<Object> boundClass = (Class<Object>) SkBeeBoundRegion.getBoundClass();
                RegionFactory.register(boundClass, SkBeeBoundRegion::new);
                logUtil.info("SkBee region adapter registered");
            } else {
                logUtil.warning("SkBee found but region adapter init failed");
            }
        }
        if (getConfig().getBoolean("regions.enable-worldguard-adapter", true)
                && Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            if (WorldGuardRegion.init()) {
                logUtil.info("WorldGuard region adapter registered");
            } else {
                logUtil.warning("WorldGuard found but region adapter init failed");
            }
        }
    }

    private void copyVanillaScripts() {
        File skgameDir = new File(Skript.getInstance().getScriptsFolder(), "skgame");
        skgameDir.mkdirs();

        String[] names = {"core", "guis", "admin"};
        Set<File> newlyCreated = new HashSet<>();

        for (String name : names) {
            if (!getConfig().getBoolean("vanilla-scripts." + name, true)) continue;
            File dest = new File(skgameDir, name + ".sk");
            if (dest.exists()) continue;
            try (InputStream in = getResource("scripts/" + name + ".sk")) {
                if (in == null) {
                    logUtil.warning("Bundled script " + name + ".sk missing from jar");
                    continue;
                }
                Files.copy(in, dest.toPath());
                newlyCreated.add(dest);
                logUtil.info("Installed vanilla script: " + name + ".sk");
            } catch (IOException e) {
                logUtil.error("Failed to install " + name + ".sk: " + e.getMessage());
            }
        }

        if (!newlyCreated.isEmpty()) {
            ScriptLoader.loadScripts(newlyCreated, OpenCloseable.EMPTY);
        }
    }

    private void loadLobbySpawn() {
        String worldName = getConfig().getString("lobby.world");
        if (worldName == null) return;
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logUtil.warning("Lobby world '" + worldName + "' not found — lobby spawn not loaded");
            return;
        }
        double x = getConfig().getDouble("lobby.x");
        double y = getConfig().getDouble("lobby.y");
        double z = getConfig().getDouble("lobby.z");
        float yaw = (float) getConfig().getDouble("lobby.yaw");
        float pitch = (float) getConfig().getDouble("lobby.pitch");
        this.lobbySpawn = new Location(world, x, y, z, yaw, pitch);
    }
}
