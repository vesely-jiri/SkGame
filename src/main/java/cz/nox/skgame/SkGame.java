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
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkGame extends JavaPlugin implements TabCompleter {

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
            "cz.nox.skgame.skript.effects.EffClearArena",
            "cz.nox.skgame.skript.effects.EffClearArenaEntityType",
            "cz.nox.skgame.skript.effects.EffMakeSpectator",
            "cz.nox.skgame.skript.effects.EffMakePlayer",
            "cz.nox.skgame.skript.effects.EffJoinAsSpectator",
            "cz.nox.skgame.skript.effects.EffSessionGameStart",
            "cz.nox.skgame.skript.effects.EffSessionGameStop",
            "cz.nox.skgame.skript.effects.EffConfigureArenaSlots",
            "cz.nox.skgame.skript.effects.EffOpenMainGui",
            "cz.nox.skgame.skript.effects.EffOpenSessionGui",
            "cz.nox.skgame.skript.effects.EffOpenMinigamesGui",
            "cz.nox.skgame.skript.effects.EffOpenMapsGui",
            "cz.nox.skgame.skript.effects.EffOpenAdminGui",
            "cz.nox.skgame.skript.effects.EffOpenPlayerProfileGui",
            // Effects — minigame section shorthands
            "cz.nox.skgame.skript.effects.EffSectionMgName",
            "cz.nox.skgame.skript.effects.EffSectionMgIcon",
            "cz.nox.skgame.skript.effects.EffSectionMgDescription",
            "cz.nox.skgame.skript.effects.EffSectionMgAuthor",
            "cz.nox.skgame.skript.effects.EffSectionMgMinPlayers",
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
            "cz.nox.skgame.skript.events.EvtMainGuiOpen",
            "cz.nox.skgame.skript.events.EvtSessionGuiOpen",
            "cz.nox.skgame.skript.events.EvtMinigamesGuiOpen",
            "cz.nox.skgame.skript.events.EvtMapsGuiOpen",
            "cz.nox.skgame.skript.events.EvtAdminGuiOpen",
            "cz.nox.skgame.skript.events.EvtPlayerProfileGuiOpen",
            // Sections
            "cz.nox.skgame.skript.sections.EffSecConfigureSession",
            "cz.nox.skgame.skript.sections.EffSecCreateSession",
            "cz.nox.skgame.skript.sections.EffSecRegisterGameMap",
            "cz.nox.skgame.skript.sections.EffSecRegisterMiniGame",
            "cz.nox.skgame.skript.sections.ExprSecCustomValue",
            "cz.nox.skgame.skript.sections.EffSecSetGameMapValueDef",
            // Expressions — sessions
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionsAll",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionFromId",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionPlayers",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionSpectators",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionValue",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionActivePlayers",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionWinners",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionArenaRegion",
            "cz.nox.skgame.skript.expressions.sessions.ExprSessionMembers",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprSessionId",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprSessionMiniGame",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprSessionMap",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprSessionHost",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprSessionState",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprSessionTotalRounds",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprSessionCurrentRound",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprAllowSpectate",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprSessionShuffle",
            "cz.nox.skgame.skript.expressions.sessions.property.ExprSessionVisibility",
            "cz.nox.skgame.skript.expressions.sessions.ExprShuffledSessionPlayers",
            // Expressions — minigames
            "cz.nox.skgame.skript.expressions.minigames.ExprMiniGameFromId",
            "cz.nox.skgame.skript.expressions.minigames.ExprMiniGameAll",
            "cz.nox.skgame.skript.expressions.minigames.ExprMiniGameValue",
            "cz.nox.skgame.skript.expressions.minigames.property.ExprMiniGameId",
            "cz.nox.skgame.skript.expressions.minigames.property.ExprMiniGameName",
            "cz.nox.skgame.skript.expressions.minigames.property.ExprMiniGameDescription",
            "cz.nox.skgame.skript.expressions.minigames.property.ExprMiniGameAuthor",
            "cz.nox.skgame.skript.expressions.minigames.property.ExprMiniGameMinPlayers",
            "cz.nox.skgame.skript.expressions.minigames.property.ExprMiniGameIcon",
            "cz.nox.skgame.skript.expressions.minigames.customValue.property.ExprCustomValueDefaultValue",
            "cz.nox.skgame.skript.expressions.minigames.customValue.property.ExprCustomValueDescription",
            "cz.nox.skgame.skript.expressions.minigames.customValue.property.ExprCustomValueName",
            "cz.nox.skgame.skript.expressions.minigames.customValue.property.ExprCustomValuePlurality",
            "cz.nox.skgame.skript.expressions.minigames.customValue.property.ExprCustomValueType",
            "cz.nox.skgame.skript.expressions.minigames.customValue.property.ExprCustomValueAllowedValues",
            "cz.nox.skgame.skript.expressions.minigames.customValue.ExprGameMapValueDef",
            // Expressions — gamemaps
            "cz.nox.skgame.skript.expressions.gamemaps.ExprGameMapFromId",
            "cz.nox.skgame.skript.expressions.gamemaps.ExprGameMapMiniGames",
            "cz.nox.skgame.skript.expressions.gamemaps.ExprGameMapAll",
            "cz.nox.skgame.skript.expressions.gamemaps.ExprGameMapValue",
            "cz.nox.skgame.skript.expressions.gamemaps.ExprGameMapMiniGameValue",
            "cz.nox.skgame.skript.expressions.gamemaps.ExprGameMapConfigValue",
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
            "cz.nox.skgame.skript.expressions.ExprLobbySpawn",
            "cz.nox.skgame.skript.expressions.ExprMaintenanceMode",
            "cz.nox.skgame.skript.expressions.ExprParty",
            "cz.nox.skgame.skript.expressions.ExprMainGuiFilter"
    );

    private static final List<String> CORE_RESOURCE_PATHS = List.of();

    private static SkGame instance;
    private SkriptAddon addon;
    private LogUtil logUtil;
    private List<SkGameModule> enabledModules;

    private final File dataFolder = new File(getDataFolder(), "storage");
    private final File miniGamesDataFile = new File(dataFolder, "minigames.yml");
    private final File mapsDataFile = new File(dataFolder, "maps.yml");
    private final File lobbyFile = new File(getDataFolder(), "lobby.yml");

    @Nullable
    private Location lobbySpawn;

    private volatile boolean maintenanceMode = false;
    private long pluginStartTime;

    public static SkGame getInstance() {
        return instance;
    }

    public LogUtil getLogUtil() {
        return logUtil;
    }

    public long getPluginStartTime() {
        return pluginStartTime;
    }

    public List<SkGameModule> getEnabledModules() {
        return enabledModules != null ? enabledModules : List.of();
    }

    public void onEnable() {
        long s = System.currentTimeMillis();
        this.pluginStartTime = s;

        instance = this;
        this.logUtil = new LogUtil(instance);

        ConfigurationSerialization.registerClass(CustomValue.class);

        if (getDataFolder().mkdirs()) {
            logUtil.info("Creating plugin folder");
        }

        saveDefaultConfig();
        migrateConfig();
        // Defer world resolution to first tick — Bukkit.getWorld() returns null during onEnable on Paper.
        Bukkit.getScheduler().runTask(this, this::loadLobbySpawn);

        this.addon = Skript.registerAddon(instance);

        // Resolve which modules are enabled based on config and soft-dep availability.
        this.enabledModules = resolveModules();

        cleanupLegacyScripts();
        new ResourceInstaller(this).installAll(CORE_RESOURCE_PATHS, enabledModules);

        // Load core Skript classes then each enabled module's declared classes.
        loadAllSkriptClasses(enabledModules);

        Bukkit.getPluginManager().registerEvents(SessionManager.getInstance(), instance);
        // Initialize lifecycle manager singleton after SessionManager is ready
        cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl lifecycle =
                cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl.getInstance();
        Bukkit.getPluginManager().registerEvents(lifecycle, instance);
        Bukkit.getPluginManager().registerEvents(new cz.nox.skgame.core.gui.GuiClickRouter(), instance);
        Bukkit.getPluginManager().registerEvents(cz.nox.skgame.core.gui.services.MainGuiService.getInstance(), instance);
        Bukkit.getPluginManager().registerEvents(cz.nox.skgame.core.gui.services.SessionGuiService.getInstance(), instance);
        Bukkit.getPluginManager().registerEvents(cz.nox.skgame.core.gui.services.MinigamesGuiService.getInstance(), instance);
        Bukkit.getPluginManager().registerEvents(cz.nox.skgame.core.gui.services.MapsGuiService.getInstance(), instance);
        Bukkit.getPluginManager().registerEvents(cz.nox.skgame.core.gui.services.AdminGuiService.getInstance(), instance);
        Bukkit.getPluginManager().registerEvents(cz.nox.skgame.core.gui.services.SpectateGuiService.getInstance(), instance);
        Bukkit.getPluginManager().registerEvents(cz.nox.skgame.core.gui.services.PlayerProfileGuiService.getInstance(), instance);
        Bukkit.getPluginManager().registerEvents(cz.nox.skgame.core.gui.services.AdminPanelGuiService.getInstance(), instance);
        Bukkit.getPluginManager().registerEvents(cz.nox.skgame.core.gui.services.GameHistoryGuiService.getInstance(), instance);
        Bukkit.getPluginManager().registerEvents(new cz.nox.skgame.core.listener.ChatIsolationListener(), instance);

        for (SkGameModule module : enabledModules) {
            module.onEnable(this);
        }

        Bukkit.getScheduler().runTaskLater(this, () -> {
            MiniGameManager.getInstance().loadFromFile(miniGamesDataFile);
            GameMapManager.getInstance().loadFromFile(mapsDataFile);
        }, 1L);

        var cmd = getCommand("skgame");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }

        cz.nox.skgame.core.command.GameCommand gameCommand = new cz.nox.skgame.core.command.GameCommand();
        var gameCmd = getCommand("game");
        if (gameCmd != null) {
            gameCmd.setExecutor(gameCommand);
            gameCmd.setTabCompleter(gameCommand);
        }

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
        YamlConfiguration cfg = new YamlConfiguration();
        if (location != null && location.getWorld() != null) {
            cfg.set("world", location.getWorld().getName());
            cfg.set("x", location.getX());
            cfg.set("y", location.getY());
            cfg.set("z", location.getZ());
            cfg.set("yaw", (double) location.getYaw());
            cfg.set("pitch", (double) location.getPitch());
        }
        try {
            cfg.save(lobbyFile);
        } catch (IOException e) {
            logUtil.error("Could not save lobby.yml: " + e.getMessage());
        }
    }

    public boolean isMaintenanceMode() {
        return maintenanceMode;
    }

    public void setMaintenanceMode(boolean value) {
        this.maintenanceMode = value;
        cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl lifecycle =
                cz.nox.skgame.core.game.lifecycle.SessionLifecycleManagerImpl.getInstance();
        if (value) {
            lifecycle.onMaintenanceEnabled();
        } else {
            lifecycle.onMaintenanceDisabled();
        }
    }

    public boolean isAllowMidGameChanges() {
        return getConfig().getBoolean("session.allow-mid-game-changes", false);
    }

    public boolean getSpectateDefaultAllow() {
        return getConfig().getBoolean("spectate.default-allow", true);
    }

    public String getSpectateBypassPermission() {
        return getConfig().getString("spectate.bypass-permission", "skgame.spectate.bypass");
    }

    public GameMode getDefaultGameMode() {
        String raw = getConfig().getString("defaults.gamemode", "adventure");
        try {
            return GameMode.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return GameMode.ADVENTURE;
        }
    }

    private static final List<String> RELOAD_COMPONENTS = List.of("all", "config", "messages", "storage", "scripts");

    @Override
    @SuppressWarnings("deprecation")
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.YELLOW + "Usage: /skgame <info|reload|maintenance|stats|panel> [args]");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "reload" -> {
                if (!sender.hasPermission("skgame.admin.reload")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }
                String component = args.length >= 2 ? args[1].toLowerCase() : "all";
                switch (component) {
                    case "all" -> { doReloadConfig(sender); doReloadMessages(sender); doReloadStorage(sender); }
                    case "config"   -> doReloadConfig(sender);
                    case "messages" -> doReloadMessages(sender);
                    case "storage"  -> doReloadStorage(sender);
                    case "scripts"  -> doReloadScripts(sender);
                    default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /skgame reload [all|config|messages|storage|scripts]");
                }
            }
            case "info" -> new cz.nox.skgame.core.command.subcommands.InfoSubcommand().execute(sender);
            case "panel" -> {
                if (!(sender instanceof org.bukkit.entity.Player player)) {
                    sender.sendMessage("Player only.");
                    return true;
                }
                if (!player.hasPermission("skgame.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                cz.nox.skgame.core.gui.services.AdminPanelGuiService.getInstance().openPanel(player);
            }
            case "stats" -> {
                if (!sender.hasPermission("skgame.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission.");
                    return true;
                }
                if (args.length < 3 || !args[1].equalsIgnoreCase("reset")) {
                    sender.sendMessage(ChatColor.YELLOW + "Usage: /skgame stats reset <player> [minigame] [confirm]");
                    return true;
                }
                handleStatsReset(sender, args);
            }
            case "maintenance" -> {
                if (!sender.hasPermission("skgame.admin")) {
                    sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
                    return true;
                }
                String sub = args.length >= 2 ? args[1].toLowerCase() : "status";
                switch (sub) {
                    case "on" -> {
                        setMaintenanceMode(true);
                        Messages.send(sender, "command.maintenance.enabled");
                        var sessions = SessionManager.getInstance().getAllSessions();
                        int totalSessions = sessions.length;
                        int multiRound = 0;
                        for (var s : sessions) {
                            if (s.getTotalRounds() > 1) multiRound++;
                        }
                        Messages.send(sender, "command.maintenance.enabled-summary",
                                totalSessions, multiRound, totalSessions - multiRound,
                                Bukkit.getOnlinePlayers().size());
                    }
                    case "off" -> {
                        setMaintenanceMode(false);
                        Messages.send(sender, "command.maintenance.disabled");
                    }
                    default -> Messages.send(sender, maintenanceMode
                            ? "command.maintenance.status-on"
                            : "command.maintenance.status-off");
                }
            }
            default -> sender.sendMessage(ChatColor.YELLOW + "Usage: /skgame <info|reload|maintenance|stats|panel> [args]");
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private void handleStatsReset(CommandSender sender, String[] args) {
        // args: [0]=stats [1]=reset [2]=playerName [3?]=minigame|confirm [4?]=confirm
        String playerName = args[2];
        String minigameId = null;
        boolean confirmed = false;

        if (args.length >= 4) {
            if (args[args.length - 1].equalsIgnoreCase("confirm")) {
                confirmed = true;
                if (args.length >= 5) minigameId = args[3].toLowerCase();
            } else {
                minigameId = args[3].toLowerCase();
            }
        }

        org.bukkit.OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if (!target.hasPlayedBefore() && !(target instanceof org.bukkit.entity.Player)) {
            Messages.send(sender, "command.stats.reset.player-not-found", playerName);
            return;
        }

        cz.nox.skgame.core.storage.GameResultsRepository repo =
                cz.nox.skgame.core.storage.GameResultsRepository.getInstance();
        int count = repo.countGamesForPlayer(target.getUniqueId(), minigameId);

        if (count == 0) {
            Messages.send(sender, "command.stats.reset.no-data", playerName);
            return;
        }
        if (!confirmed) {
            String scope = minigameId != null ? " (minigame: " + minigameId + ")" : "";
            Messages.send(sender, "command.stats.reset.confirm-prompt", count, playerName + scope);
            return;
        }

        String finalMinigameId = minigameId;
        String finalName = playerName;
        java.util.UUID uuid = target.getUniqueId();
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            int deleted = repo.deletePlayerStats(uuid, finalMinigameId);
            Bukkit.getScheduler().runTask(this, () ->
                    Messages.send(sender, "command.stats.reset.done", deleted, finalName));
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            List<String> opts = new ArrayList<>();
            if (sender.hasPermission("skgame.info")) opts.add("info");
            if (sender.hasPermission("skgame.admin.reload")) opts.add("reload");
            if (sender.hasPermission("skgame.admin")) { opts.add("maintenance"); opts.add("stats"); opts.add("panel"); }
            return StringUtil.copyPartialMatches(args[0], opts, new ArrayList<>());
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase()) {
                case "reload" -> sender.hasPermission("skgame.admin.reload")
                        ? StringUtil.copyPartialMatches(args[1], RELOAD_COMPONENTS, new ArrayList<>())
                        : Collections.emptyList();
                case "maintenance" -> sender.hasPermission("skgame.admin")
                        ? StringUtil.copyPartialMatches(args[1], List.of("on", "off", "status"), new ArrayList<>())
                        : Collections.emptyList();
                default -> Collections.emptyList();
            };
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("deprecation")
    private void doReloadConfig(CommandSender sender) {
        reloadConfig();
        sender.sendMessage(ChatColor.GREEN + "config.yml reloaded.");
    }

    @SuppressWarnings("deprecation")
    private void doReloadMessages(CommandSender sender) {
        if (!getConfig().getBoolean("messages.hot-reload", true)) {
            sender.sendMessage(ChatColor.RED + "Hot-reload is disabled (messages.hot-reload: false in config.yml).");
            return;
        }
        boolean messagesEnabled = enabledModules != null &&
                enabledModules.stream().anyMatch(m -> m.getId().equals("messages"));
        if (!messagesEnabled) {
            sender.sendMessage(ChatColor.RED + "Messages module is not enabled.");
            return;
        }
        File messagesDir = new File(getDataFolder(), "messages");
        Messages.load(messagesDir, getConfig(), getLogger());
        int loaded = Messages.getLoadedLocales().size();
        sender.sendMessage(ChatColor.GREEN + "Messages reloaded — " + loaded
                + " locale(s): " + Messages.getLoadedLocales());
    }

    @SuppressWarnings("deprecation")
    private void doReloadStorage(CommandSender sender) {
        MiniGameManager.getInstance().loadFromFile(miniGamesDataFile);
        GameMapManager.getInstance().loadFromFile(mapsDataFile);
        sender.sendMessage(ChatColor.GREEN + "Storage reloaded (minigames.yml, maps.yml).");
    }

    @SuppressWarnings("deprecation")
    private void doReloadScripts(CommandSender sender) {
        Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "skript reload skgame");
        sender.sendMessage(ChatColor.GREEN + "Dispatched: skript reload skgame");
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
        } catch (ExceptionInInitializerError | NoClassDefFoundError e) {
            // Static initializer threw (e.g. unregistered return type passed to Skript.registerExpression).
            // Log and continue — do not let one bad class abort loading of subsequent classes.
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            logUtil.error("Skript class failed to initialize: " + fqn + " — " + cause);
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

    private void cleanupLegacyScripts() {
        File skgameScripts = new File(Skript.getInstance().getScriptsFolder(), "skgame");
        for (String name : new String[]{"core.sk", "guis.sk", "admin.sk"}) {
            File f = new File(skgameScripts, name);
            if (!f.exists()) continue;
            if (f.delete()) {
                logUtil.warning("Deleted legacy script '" + name + "' — no longer bundled. Update any custom scripts that referenced it.");
            } else {
                logUtil.warning("Could not delete legacy script '" + name + "' — delete it manually to avoid Skript parse errors.");
            }
        }
    }

    private void loadLobbySpawn() {
        if (!lobbyFile.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(lobbyFile);
        String worldName = cfg.getString("world");
        if (worldName == null) return;
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logUtil.warning("Lobby world '" + worldName + "' not found — lobby spawn not loaded");
            return;
        }
        double x = cfg.getDouble("x");
        double y = cfg.getDouble("y");
        double z = cfg.getDouble("z");
        float yaw = (float) cfg.getDouble("yaw");
        float pitch = (float) cfg.getDouble("pitch");
        this.lobbySpawn = new Location(world, x, y, z, yaw, pitch);
    }
}
