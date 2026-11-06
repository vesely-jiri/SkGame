package cz.nox.skgame.core.scoreboard;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.game.event.GameStartEvent;
import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.io.File;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ScoreboardService implements Listener {

    private static ScoreboardService instance;

    private final ConcurrentHashMap<String, SessionScoreboard> boards = new ConcurrentHashMap<>();
    /** Transient pre-game (PREPARATION/STARTING) countdown boards, keyed by session id. */
    private final ConcurrentHashMap<String, PreparationScoreboard> prepBoards = new ConcurrentHashMap<>();
    private static final List<String> PREP_HEADER = List.of("&8&m                    ");
    private static final List<String> PREP_FOOTER = List.of("&8&m                    ");
    private volatile ScoreboardConfig config = ScoreboardConfig.defaultConfig();

    private ScoreboardService() {}

    public static synchronized ScoreboardService getInstance() {
        if (instance == null) instance = new ScoreboardService();
        return instance;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    public void init(SkGame plugin) {
        File file = new File(plugin.getDataFolder(), "scoreboard.yml");
        if (!file.exists()) plugin.saveResource("scoreboard.yml", false);
        config = loadConfig(file, plugin);
    }

    public void reload(SkGame plugin) {
        File file = new File(plugin.getDataFolder(), "scoreboard.yml");
        config = loadConfig(file, plugin);
        // Does NOT re-render existing boards; applies on next board creation.
    }

    private static ScoreboardConfig loadConfig(File file, SkGame plugin) {
        try {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            String title = yml.getString("title", "&6Event Server");
            List<String> lines = yml.getStringList("lines");
            if (lines.isEmpty()) lines = List.of("{content}");
            return ScoreboardConfig.parse(title, lines);
        } catch (Exception e) {
            plugin.getLogUtil().warning("scoreboard.yml load failed: " + e.getMessage() + " — using defaults");
            return ScoreboardConfig.defaultConfig();
        }
    }

    // ─── API (called from Skript effects + SessionLifecycleManagerImpl) ───────

    /** Set content lines for a session's board. No-op if no board exists. */
    public void setContent(Session session, List<String> lines) {
        SessionScoreboard board = boards.get(session.getId());
        if (board != null) board.setContent(lines);
    }

    /** Restore a single player's captured board. No-op if no board exists for the session. */
    public void hideFromIfPresent(Player player, Session session) {
        SessionScoreboard board = boards.get(session.getId());
        if (board != null) board.hideFromIfPresent(player);
    }

    /** Dispose the session's board and restore all players. No-op if absent. */
    public void disposeIfPresent(Session session) {
        SessionScoreboard board = boards.remove(session.getId());
        if (board != null) board.dispose();
    }

    // ─── Pre-game countdown board (PREPARATION / STARTING) ─────────────────────

    /** Start (or restart) a per-player pre-game countdown board for the session. */
    public PreparationScoreboard startPreparationBoard(Session session, String titleLegacy) {
        disposePreparationBoard(session);
        PreparationScoreboard board = new PreparationScoreboard(session.getId(), titleLegacy, PREP_HEADER, PREP_FOOTER);
        prepBoards.put(session.getId(), board);
        return board;
    }

    /** The session's active pre-game board, or null if none. */
    public PreparationScoreboard getPreparationBoard(Session session) {
        return prepBoards.get(session.getId());
    }

    /** Dispose the session's pre-game board and restore all players. No-op if absent. Idempotent. */
    public void disposePreparationBoard(Session session) {
        PreparationScoreboard board = prepBoards.remove(session.getId());
        if (board != null) board.dispose();
    }

    /** Restore a single player's board from the session's pre-game board. No-op if absent. */
    public void hidePreparationFrom(Player player, Session session) {
        PreparationScoreboard board = prepBoards.get(session.getId());
        if (board != null) board.removePlayer(player);
    }

    // ─── Listener ────────────────────────────────────────────────────────────

    @EventHandler
    public void onGameStart(GameStartEvent event) {
        Session session = event.getSession();
        MiniGame mg = session.getMiniGame();
        if (mg == null) return;

        Object boardMode = mg.getValue("scoreboard");
        if (boardMode == null) return;

        String mode = boardMode.toString().trim().toLowerCase();
        if ("per-player".equals(mode)) {
            SkGame.getInstance().getLogUtil().warning(
                    "[ScoreboardService] per-player scoreboard not yet implemented; falling back to per-session");
            mode = "per-session";
        }
        if (!"per-session".equals(mode)) return;

        SessionScoreboard board = new SessionScoreboard(session.getId(), config);
        boards.put(session.getId(), board);
        for (Player p : session.getPlayers()) board.showTo(p);
    }
}
