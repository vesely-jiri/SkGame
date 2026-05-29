package cz.nox.skgame.util;

import cz.nox.skgame.SkGame;

import java.util.function.Supplier;

public final class Debug {

    private Debug() {}

    public static void log(String category, String message) {
        if (!SkGame.getInstance().getConfig().getBoolean("debug", false)) return;
        SkGame.getInstance().getLogger().info("[DEBUG][" + category + "] " + message);
    }

    public static void log(String category, Supplier<String> message) {
        if (!SkGame.getInstance().getConfig().getBoolean("debug", false)) return;
        SkGame.getInstance().getLogger().info("[DEBUG][" + category + "] " + message.get());
    }

    /**
     * Per-minigame debug log. Fires if global debug is on OR the minigame is per-minigame-debugged.
     * Echoes to the watcher admin in-game when per-minigame mode is active.
     * Lazy Supplier — no string built when both gates are off.
     */
    public static void logMiniGame(String minigameId, String category, Supplier<String> message) {
        SkGame plugin = SkGame.getInstance();
        boolean globalOn = plugin.getConfig().getBoolean("debug", false);
        boolean mgOn = plugin.isMinigameDebugged(minigameId);
        if (!globalOn && !mgOn) return;
        String line = "[DEBUG][" + category + "][" + minigameId + "] " + message.get();
        plugin.getLogger().info(line);
        if (mgOn) {
            java.util.UUID watcherId = plugin.getDebugWatcher(minigameId);
            if (watcherId != null) {
                org.bukkit.entity.Player watcher = org.bukkit.Bukkit.getPlayer(watcherId);
                if (watcher != null && watcher.isOnline()) watcher.sendMessage(line);
            }
        }
    }
}
