package cz.nox.skgame;

import ch.njol.skript.Skript;
import ch.njol.skript.ScriptLoader;
import ch.njol.skript.SkriptAddon;
import ch.njol.util.OpenCloseable;
import cz.nox.skgame.api.game.model.CustomValue;
import cz.nox.skgame.api.messages.Messages;
import cz.nox.skgame.api.module.SkGameModule;
import cz.nox.skgame.core.game.GameMapManager;
import cz.nox.skgame.core.game.MiniGameManager;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.module.ModuleRegistry;
import cz.nox.skgame.core.module.ResourceInstaller;
import cz.nox.skgame.util.LogUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkGame extends JavaPlugin {

    // All Skript syntax classes that are always-on (not owned by any optional module).
    private static final List<String> CORE_SKRIPT_CLASSES = List.of(
            "cz.nox.skgame.skript.Types",
            // Conditions
            "cz.nox.skgame.skript.conditions.CondSessionExists",
            "cz.nox.skgame.skript.conditions.CondGameMapSupportsMiniGame",
            "cz.nox.skgame.skript.conditions.CondIsPlaying",
            "cz.nox.skgame.skript.conditions.CondIsInSession",
            "cz.nox.skgame.skript.conditions.CondIsMapTaken",
            "cz.nox.skgame.skript.conditions.CondIsCustomValue",
            "cz.nox.skgame.skript.conditions.CondIsSpectator",
            "cz.nox.skgame.skript.conditions.CondIsPlayer",
            // Effects
            "cz.nox.skgame.skript.effects.EffUnregisterMiniGame",
            "cz.nox.skgame.skript.effects.EffUnregisterMap",
            "cz.nox.skgame.skript.effects.EffSessionDisband",
            "cz.nox.skgame.skript.effects.EffSessionCancelCountdown",
            "cz.nox.skgame.skript.effects.EffResetPlayer",
            "cz.nox.skgame.skript.effects.EffTeleportToLobby",
            "cz.nox.skgame.skript.effects.EffClearRegion",
            "cz.nox.skgame.skript.effects.EffMakeSpectator",
            "cz.nox.skgame.skript.effects.EffMakePlayer",
            "cz.nox.skgame.skript.effects.EffJoinAsSpectator",
            "cz.nox.skgame.skript.effects.EffSessionGameStart",
            "cz.nox.skgame.skript.effects.EffSessionGameStop",
            "cz.nox.skgame.skript.effects.EffConfigureArenaSlots",
            // Events
            "cz.nox.skgame.skript.events.EvtSessionCreate",
            "cz.nox.skgame.skript.events.EvtPlayerSessionLeave",
            "cz.nox.skgame.skript.events.EvtPlayerSessionJoin",
            "cz.nox.skgame.skript.events.EvtGameStop",
            "cz.nox.skgame.skript.events.EvtGameStart",
            "cz.nox.skgame.skript.events.EvtSessionDisband",
            "cz.nox.skgame.skript.events.EvtSpectatorJoin",
            "cz.nox.skgame.skript.events.EvtPlayerPromoted",
            "cz.nox.skgame.skript.events.EvtPlayerDemoted",
            "cz.nox.skgame.skript.events.EvtLobbyEnter",
            // Sections
            "cz.nox.skgame.skript.sections.EffSecCreateSession",
            "cz.nox.skgame.skript.sections.EffSecRegisterGameMap",
            "cz.nox.skgame.skript.sections.EffSecRegisterMiniGame",
            "cz.nox.skgame.skript.sections.ExprSecCustomValue",
            // Expressions — sessions
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionsAll",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionFromId",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionPlayers",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionSpectators",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionValue",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionActivePlayers",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionArenaRegion",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionMembers",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprSessionId",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprSessionMiniGame",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprSessionMap",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprSessionHost",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprSessionState",
            // Expressions — minigames
            "cz.nox.skgame.skript.expressions.minigames.ExprMiniGameFromId",
            "cz.nox.skgame.skript.expressions.minigames.ExprMiniGameAll",
            "cz.nox.skgame.skript.expressions.minigames.ExprMiniGameValue",
            "cz.nox.skgame.skript.expressions.minigames.property.ExprMiniGameId",
            "cz.nox.skgame.skript.expressions.minigames.customValue.property.ExprCustomValueDefaultValue",
            "cz.nox.skgame.skript.expressions.minigames.customValue.property.ExprCustomValueDescription",
            "cz.nox.skgame.skript.expressions.minigames.customValue.property.ExprCustomValueName",
            "cz.nox.skgame.skript.expressions.minigames.customValue.property.ExprCustomValuePlurality",
            "cz.nox.skgame.skript.expressions.minigames.customValue.property.ExprCustomValueType",
            // Expressions — gamemaps
            "cz.nox.skgame.skript.expressions.gamemaps.ExprGameMapFromId",
            "cz.nox.skgame.skript.expressions.gamemaps.ExprGameMapMiniGames",
            "cz.nox.skgame.skript.expressions.gamemaps.ExprGameMapAll",
            "cz.nox.skgame.skript.expressions.gamemaps.ExprGameMapValue",
            "cz.nox.skgame.skript.expressions.gamemaps.ExprGameMapMiniGameValue",
            "cz.nox.skgame.skript.expressions.gamemaps.ExprGameMapRegion",
            "cz.nox.skgame.skript.expressions.gamemaps.ExprGameMapArenaSlotCount",
            "cz.nox.skgame.skript.expressions.gamemaps.property.ExprGameMapId",
            // Expressions — gameplayers
            "cz.nox.skgame.skript.expressions.gameplayers.ExprGamePlayerValue",
            "cz.nox.skgame.skript.expressions.gameplayers.ExprGamePlayerSession",
            // Expressions — regions (core subset, excludes SkBee and WG adapters)
            "cz.nox.skgame.skript.expressions.regions.ExprRegionPlayers",
            "cz.nox.skgame.skript.expressions.regions.ExprRegionEntities",
            "cz.nox.skgame.skript.expressions.regions.ExprRegionBlocks",
            "cz.nox.skgame.skript.expressions.regions.ExprRegionCenter",
            "cz.nox.skgame.skript.expressions.regions.ExprCuboidRegion",
            // Expressions — misc
            "cz.nox.skgame.skript.expressions.ExprLobbySpawn"
    );

    // core.sk is always installed — user minigames depend on its helper functions.
    private static final List<String> CORE_RESOURCE_PATHS = List.of("scripts/core.sk");

    private static SkGame instance;
    private SkriptAddon addon;
    private LogUtil logUtil;
    private List<SkGameModule> enabledModules;

    private final File dataFolder = new File(getDataFolder(), "storage");
    private final File miniGamesDataFile = new File(dataFolder, "minigames.yml");
    private final File mapsDataFile = new File(dataFolder, "maps.yml");

    @Nullable
    private Location lobbySpawn;

    public static SkGame getInstance() {
        return instance;
    }

    public LogUtil getLogUtil() {
        return logUtil;
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
        migrateConfig();
        loadLobbySpawn();

        this.addon = Skript.registerAddon(instance);

        // Resolve which modules are enabled based on config and soft-dep availability.
        this.enabledModules = resolveModules();

        // Install core.sk always, then each enabled module's declared resources.
        new ResourceInstaller(this).installAll(CORE_RESOURCE_PATHS, enabledModules);

        // Load core Skript classes then each enabled module's declared classes.
        loadAllSkriptClasses(enabledModules);

        Bukkit.getPluginManager().registerEvents(SessionManager.getInstance(), instance);
        // Initialize lifecycle manager singleton after SessionManager is ready
        cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl.getInstance();

        for (SkGameModule module : enabledModules) {
            module.onEnable(this);
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            MiniGameManager.getInstance().loadFromFile(miniGamesDataFile);
            GameMapManager.getInstance().loadFromFile(mapsDataFile);
        }, 1L);

        var cmd = getCommand("skgame");
        if (cmd != null) cmd.setExecutor(this);

        logUtil.info("SkGame enabled in " + (System.currentTimeMillis() - s) + "ms");
    }

    public void onDisable() {
        long s = System.currentTimeMillis();

        MiniGameManager.getInstance().saveToFile(miniGamesDataFile);
        GameMapManager.getInstance().saveToFile(mapsDataFile);

        // Disband all live sessions before modules unload
        cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl.getInstance().shutdown();

        if (enabledModules != null) {
            List<SkGameModule> reversed = new ArrayList<>(enabledModules);
            Collections.reverse(reversed);
            for (SkGameModule module : reversed) {
                module.onDisable(this);
            }
        }

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

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("skgame.admin.reload")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("reload") && args[1].equalsIgnoreCase("messages")) {
            if (!getConfig().getBoolean("messages.hot-reload", true)) {
                sender.sendMessage(ChatColor.RED + "Hot-reload is disabled (messages.hot-reload: false in config.yml).");
                return true;
            }
            boolean messagesEnabled = enabledModules != null &&
                    enabledModules.stream().anyMatch(m -> m.getId().equals("messages"));
            if (!messagesEnabled) {
                sender.sendMessage(ChatColor.RED + "Messages module is not enabled.");
                return true;
            }
            File messagesDir = new File(getDataFolder(), "messages");
            Messages.load(messagesDir, getConfig(), getLogger());
            int loaded = Messages.getLoadedLocales().size();
            sender.sendMessage(ChatColor.GREEN + "Messages reloaded — " + loaded
                    + " locale(s): " + Messages.getLoadedLocales());
            return true;
        }
        sender.sendMessage(ChatColor.YELLOW + "Usage: /skgame reload messages");
        return true;
    }

    private List<SkGameModule> resolveModules() {
        List<SkGameModule> enabled = new ArrayList<>();
        Set<String> enabledIds = new HashSet<>();

        for (SkGameModule module : ModuleRegistry.BUILTIN_MODULES) {
            String key = "modules." + module.getId() + ".enabled";
            boolean configEnabled = getConfig().getBoolean(key, module.isEnabledByDefault());

            if (!configEnabled) {
                logUtil.info("Module " + module.getId() + " disabled in config");
                continue;
            }

            boolean depsOk = true;
            for (String dep : module.getDependencies()) {
                if (!enabledIds.contains(dep)) {
                    logUtil.warning("Module " + module.getId() + " requires module '" + dep + "' which is not enabled — skipping");
                    depsOk = false;
                    break;
                }
            }
            if (!depsOk) continue;

            if (!module.canEnable(this)) continue;

            enabled.add(module);
            enabledIds.add(module.getId());
            logUtil.info("Module " + module.getId() + " enabled");
        }

        return enabled;
    }

    private void loadAllSkriptClasses(List<SkGameModule> modules) {
        for (String fqn : CORE_SKRIPT_CLASSES) {
            loadSkriptClass(fqn);
        }
        for (SkGameModule module : modules) {
            for (String fqn : module.getSkriptClasses()) {
                loadSkriptClass(fqn);
            }
        }
    }

    private void loadSkriptClass(String fqn) {
        try {
            Class.forName(fqn, true, getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            logUtil.error("Skript class not found: " + fqn);
        }
    }

    private void migrateConfig() {
        boolean changed = false;

        // vanilla-scripts.* → modules.*.enabled  (phase 1-4 config format)
        if (getConfig().isSet("vanilla-scripts")) {
            if (getConfig().isSet("vanilla-scripts.guis")) {
                getConfig().set("modules.gui.enabled", getConfig().getBoolean("vanilla-scripts.guis", true));
            }
            if (getConfig().isSet("vanilla-scripts.admin")) {
                getConfig().set("modules.admin.enabled", getConfig().getBoolean("vanilla-scripts.admin", true));
            }
            // vanilla-scripts.core has no module equivalent — core.sk is always installed.
            getConfig().set("vanilla-scripts", null);
            changed = true;
            logUtil.info("Migrated vanilla-scripts config to modules config");
        }

        // regions.* → modules.region-*.enabled  (phase 1-4 config format)
        if (getConfig().isSet("regions")) {
            if (getConfig().isSet("regions.enable-skbee-adapter")) {
                getConfig().set("modules.region-skbee.enabled", getConfig().getBoolean("regions.enable-skbee-adapter", true));
            }
            if (getConfig().isSet("regions.enable-worldguard-adapter")) {
                getConfig().set("modules.region-worldguard.enabled", getConfig().getBoolean("regions.enable-worldguard-adapter", true));
            }
            getConfig().set("regions", null);
            changed = true;
            logUtil.info("Migrated regions config to modules config");
        }

        if (changed) saveConfig();
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
