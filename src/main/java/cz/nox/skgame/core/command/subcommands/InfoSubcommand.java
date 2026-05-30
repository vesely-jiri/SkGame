package cz.nox.skgame.core.command.subcommands;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.SessionState;
import cz.nox.skgame.api.module.SkGameModule;
import cz.nox.skgame.core.game.MiniGameManager;
import cz.nox.skgame.core.game.SessionManager;
import cz.nox.skgame.core.storage.DatabaseManager;
import cz.nox.skgame.util.BuildInfo;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InfoSubcommand {

    private static final String GITHUB_URL = "https://github.com/vesely-jiri/SkGame";

    public void execute(CommandSender sender) {
        if (!sender.hasPermission("skgame.info")) {
            send(sender, c("&cYou don't have permission to do this"));
            return;
        }
        SkGame plugin = SkGame.getInstance();
        boolean extended = sender.hasPermission("skgame.info.extended");

        // Header with clickable version + git SHA
        String ver = plugin.getDescription().getVersion();
        String sha = BuildInfo.gitSha();
        String shaDisplay = sha.length() > 7 ? sha.substring(0, 7) : sha;

        Component versionChip = Component.text("[SkGame " + ver + "]", NamedTextColor.AQUA, TextDecoration.BOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Open GitHub repository", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.openUrl(GITHUB_URL));
        String commitUrl = GITHUB_URL + "/commit/" + sha;
        Component shaChip = Component.text("[git:" + shaDisplay + "]", NamedTextColor.DARK_GRAY)
                .hoverEvent(HoverEvent.showText(Component.text("Click to view commit on GitHub", NamedTextColor.GRAY)))
                .clickEvent(ClickEvent.openUrl(commitUrl));

        send(sender, versionChip
                .append(Component.text(" ", NamedTextColor.WHITE))
                .append(shaChip));

        // Server line
        Plugin skript = Bukkit.getPluginManager().getPlugin("Skript");
        String skriptVer = skript != null ? skript.getDescription().getVersion() : "unknown";
        send(sender, c("&7Server: &f" + Bukkit.getServer().getName()
                + " " + Bukkit.getServer().getVersion()
                + " &7| Skript &f" + skriptVer));

        // Modules
        List<SkGameModule> modules = plugin.getEnabledModules();
        String moduleList = modules.isEmpty() ? "none"
                : modules.stream().map(SkGameModule::getId).collect(Collectors.joining(", "));
        send(sender, c("&7Modules: &f" + moduleList));

        // Minigames
        MiniGame[] mgs = MiniGameManager.getInstance().getAllMiniGames();
        String mgNames = mgs.length == 0 ? "none"
                : Arrays.stream(mgs).map(mg -> {
            Object n = mg.getValue("name");
            return n != null ? n.toString() : mg.getId();
        }).collect(Collectors.joining(", "));
        send(sender, c("&7Minigames &8(" + mgs.length + ")&7: &f" + mgNames));

        // Sessions
        Session[] all = SessionManager.getInstance().getAllSessions();
        long lobby = Arrays.stream(all).filter(s -> s.getState() == SessionState.LOBBY).count();
        long running = Arrays.stream(all)
                .filter(s -> s.getState() == SessionState.STARTED || s.getState() == SessionState.STARTING)
                .count();
        send(sender, c("&7Sessions: &f" + all.length
                + " &8(lobby: &f" + lobby + "&8, running: &f" + running + "&8)"));

        if (!extended) return;

        // Extended: runtime
        long uptimeMs = System.currentTimeMillis() - plugin.getPluginStartTime();
        send(sender, c("&7Uptime: &f" + formatUptime(uptimeMs)
                + " &7| Maintenance: " + (plugin.isMaintenanceMode() ? "&cON" : "&aOFF")));

        Location lobbySpawn = plugin.getLobbySpawn();
        if (lobbySpawn != null) {
            String world = lobbySpawn.getWorld() != null ? lobbySpawn.getWorld().getName() : "?";
            send(sender, c("&7Lobby spawn: &f" + world
                    + String.format(" %.1f %.1f %.1f", lobbySpawn.getX(), lobbySpawn.getY(), lobbySpawn.getZ())));
        } else {
            send(sender, c("&7Lobby spawn: &cnot set"));
        }

        // Extended: database
        DatabaseManager db = DatabaseManager.getInstance();
        if (db.isAvailable()) {
            long rows = db.getTotalGameResultCount();
            String rowStr = rows >= 0 ? String.format("%,d", rows) : "unknown";
            File dbFile = db.getDbFile();
            String sizeStr = dbFile != null && dbFile.exists() ? " | " + formatFileSize(dbFile.length()) : "";
            send(sender, c("&7Database: &aconnected &7| &f" + rowStr + " results" + sizeStr));
        } else {
            send(sender, c("&7Database: &cdisconnected"));
        }

        // Update checker result
        String updateVer = plugin.getUpdateAvailableVersion();
        if (updateVer != null) {
            String updateUrl = plugin.getUpdateHtmlUrl();
            send(sender, c("&eUpdate available: &a" + updateVer
                    + " &7— " + (updateUrl != null ? updateUrl : GITHUB_URL)));
        }
    }

    private static String formatUptime(long ms) {
        long s = ms / 1000;
        long d = s / 86400, h = (s % 86400) / 3600, m = (s % 3600) / 60, sec = s % 60;
        if (d > 0) return d + "d " + h + "h " + m + "m";
        if (h > 0) return h + "h " + m + "m " + sec + "s";
        if (m > 0) return m + "m " + sec + "s";
        return sec + "s";
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private static Component c(String legacy) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(legacy);
    }

    private static void send(CommandSender sender, String legacy) {
        send(sender, c(legacy));
    }

    @SuppressWarnings({"deprecation", "UnstableApiUsage"})
    private static void send(CommandSender sender, Component component) {
        if (sender instanceof Player p) {
            p.sendMessage(component);
        } else {
            // Console: strip to plain text via legacy serializer
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().serialize(component));
        }
    }
}
