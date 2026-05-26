package cz.nox.skgame.core.command.subcommands;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.module.SkGameModule;
import cz.nox.skgame.core.game.MiniGameManager;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.storage.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InfoSubcommand {

    public void execute(CommandSender sender) {
        SkGame plugin = SkGame.getInstance();
        boolean extended = sender.hasPermission("skgame.info.extended");

        send(sender, "&3&l===== SkGame Info =====");

        // SkGame
        send(sender, "&3SkGame:");
        send(sender, "  &7Version: &f" + plugin.getDescription().getVersion());

        // Server
        send(sender, "&3Server:");
        send(sender, "  &7Server: &f" + Bukkit.getServer().getName() + " " + Bukkit.getServer().getVersion());
        Plugin skriptPlugin = Bukkit.getPluginManager().getPlugin("Skript");
        String skriptVer = skriptPlugin != null ? skriptPlugin.getDescription().getVersion() : "unknown";
        send(sender, "  &7Skript: &f" + skriptVer);

        // Modules
        List<SkGameModule> modules = plugin.getEnabledModules();
        String moduleList = modules.isEmpty() ? "none"
                : modules.stream().map(SkGameModule::getId).collect(Collectors.joining(", "));
        send(sender, "&3Modules:");
        send(sender, "  &7Enabled: &f" + moduleList);

        // Minigames
        MiniGame[] minigames = MiniGameManager.getInstance().getAllMiniGames();
        send(sender, "&3Minigames &8(" + minigames.length + ")&3:");
        if (minigames.length == 0) {
            send(sender, "  &7(none registered)");
        } else {
            for (MiniGame mg : minigames) {
                Object nameObj = mg.getValue("name");
                String displayName = nameObj != null ? nameObj.toString() : mg.getId();
                send(sender, "  &7- &f" + displayName + " &8[" + mg.getId() + "]");
            }
        }

        // Sessions
        Session[] all = SessionManager.getInstance().getAllSessions();
        long lobby = Arrays.stream(all).filter(s -> s.getState() == SessionState.LOBBY).count();
        long running = Arrays.stream(all)
                .filter(s -> s.getState() == SessionState.STARTED || s.getState() == SessionState.STARTING)
                .count();
        send(sender, "&3Sessions:");
        send(sender, "  &7Active: &f" + all.length
                + " &7(lobby: &f" + lobby + "&7, running: &f" + running + "&7)");

        if (!extended) return;

        // Runtime
        send(sender, "&3Runtime &8(extended)&3:");
        long uptimeMs = System.currentTimeMillis() - plugin.getPluginStartTime();
        send(sender, "  &7Uptime: &f" + formatUptime(uptimeMs));
        send(sender, "  &7Maintenance: " + (plugin.isMaintenanceMode() ? "&cON" : "&aOFF"));
        Location lobbySpawn = plugin.getLobbySpawn();
        if (lobbySpawn != null) {
            String worldName = lobbySpawn.getWorld() != null ? lobbySpawn.getWorld().getName() : "?";
            send(sender, "  &7Lobby spawn: &f" + worldName
                    + " " + String.format("%.1f", lobbySpawn.getX())
                    + " " + String.format("%.1f", lobbySpawn.getY())
                    + " " + String.format("%.1f", lobbySpawn.getZ()));
        } else {
            send(sender, "  &7Lobby spawn: &cnot set");
        }

        // Database
        DatabaseManager db = DatabaseManager.getInstance();
        send(sender, "&3Database &8(extended)&3:");
        if (db.isAvailable()) {
            send(sender, "  &7Status: &aconnected");
            long rowCount = db.getTotalGameResultCount();
            send(sender, "  &7Game results stored: &f" + (rowCount >= 0 ? rowCount : "unknown"));
            File dbFile = db.getDbFile();
            if (dbFile != null && dbFile.exists()) {
                send(sender, "  &7DB file size: &f" + formatFileSize(dbFile.length()));
            }
        } else {
            send(sender, "  &7Status: &cdisconnected");
        }
    }

    private static String formatUptime(long ms) {
        long seconds = ms / 1000;
        long d = seconds / 86400;
        long h = (seconds % 86400) / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (d > 0) return d + "d " + h + "h " + m + "m";
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    private static void send(CommandSender sender, String msg) {
        if (sender instanceof Player p) {
            p.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
        } else {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }
    }
}
