package cz.nox.skgame.core.scoreboard;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import cz.nox.skgame.util.Debug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-session sidebar scoreboard.
 * Uses Team-per-line to bypass the 16-char entry display limit (Paper 1.21 Adventure).
 * Flicker-free: reuses Score entries, only updates changed team prefixes.
 */
public class SessionScoreboard {

    private static final int MAX_LINES = 15;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final Scoreboard board;
    private final Objective objective;
    /** Line index → Team. Teams are created lazily. */
    private final Map<Integer, Team> lineTeams = new HashMap<>();
    /** Player UUID → their scoreboard captured before we showed ours. */
    private final Map<UUID, Scoreboard> capturedBoards = new HashMap<>();
    private List<String> content = List.of();
    private ScoreboardConfig config;
    /** Guard: only warn about overflow once per board lifetime. */
    private boolean overflowWarned = false;
    private final String sessionId;

    @SuppressWarnings("deprecation")
    public SessionScoreboard(String sessionId, ScoreboardConfig config) {
        this.sessionId = sessionId;
        this.config = config;
        this.board = Bukkit.getScoreboardManager().getNewScoreboard();
        this.objective = board.registerNewObjective(
                "skgb",
                "dummy",
                LEGACY.deserialize(config.title())
        );
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    /** Replace displayed content lines. Re-renders immediately. */
    public void setContent(List<String> newContent) {
        this.content = List.copyOf(newContent);
        renderLines();
    }

    /** Update layout config (called on /skgame reload; applies to next setContent call). */
    public void updateConfig(ScoreboardConfig cfg) {
        this.config = cfg;
        objective.displayName(LEGACY.deserialize(cfg.title()));
        renderLines();
    }

    /** Show this board to a player, capturing their current board for later restore. */
    public void showTo(Player player) {
        if (!player.isOnline()) return;
        capturedBoards.put(player.getUniqueId(), player.getScoreboard());
        player.setScoreboard(board);
    }

    /**
     * Restore a player's captured board (best-effort).
     * No-op if the player is no longer on our board (e.g. another plugin replaced it).
     */
    public void hideFrom(Player player) {
        Scoreboard captured = capturedBoards.remove(player.getUniqueId());
        if (captured == null) return;
        if (player.isOnline() && player.getScoreboard() == board) {
            try {
                player.setScoreboard(captured);
            } catch (Exception ignored) {
                // Captured board may have been unregistered by now — fall back to main
                player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
    }

    /** Hide from this specific player if they were shown this board. */
    public void hideFromIfPresent(Player player) {
        if (capturedBoards.containsKey(player.getUniqueId())) hideFrom(player);
    }

    /** Restore all players' boards and clean up the objective. */
    public void dispose() {
        for (UUID uuid : new ArrayList<>(capturedBoards.keySet())) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) hideFrom(p);
        }
        capturedBoards.clear();
        try { objective.unregister(); } catch (IllegalStateException ignored) {}
    }

    // ─── Rendering ───────────────────────────────────────────────────────────

    @SuppressWarnings("deprecation")
    private void renderLines() {
        List<String> merged = new ArrayList<>();
        merged.addAll(config.headerLines());
        merged.addAll(content);
        merged.addAll(config.footerLines());

        int total = merged.size();
        if (total > MAX_LINES) {
            if (!overflowWarned) {
                overflowWarned = true;
                Debug.log("scoreboard", () -> "session=" + sessionId
                        + " overflow: total=" + total + " > 15; dropping " + (total - MAX_LINES) + " content lines");
            }
            // Keep header + footer whole; truncate content
            int maxContent = MAX_LINES - config.headerLines().size() - config.footerLines().size();
            if (maxContent < 0) maxContent = 0;
            List<String> truncated = content.size() > maxContent ? content.subList(0, maxContent) : content;
            merged = new ArrayList<>();
            merged.addAll(config.headerLines());
            merged.addAll(truncated);
            merged.addAll(config.footerLines());
        }

        int rendered = merged.size();

        for (int i = 0; i < rendered; i++) {
            String text = merged.get(i);
            String dummyKey = ChatColor.values()[i].toString();
            Team team = lineTeams.computeIfAbsent(i, idx -> {
                Team t = board.registerNewTeam("sl" + idx);
                t.addEntry(ChatColor.values()[idx].toString());
                return t;
            });
            team.prefix(LEGACY.deserialize(text));
            // Score = descending so line 0 is at top
            objective.getScore(dummyKey).setScore(MAX_LINES - i);
        }

        // Remove stale lines from a prior longer render
        for (int i = rendered; i < MAX_LINES; i++) {
            Team stale = lineTeams.remove(i);
            if (stale != null) {
                String dummyKey = ChatColor.values()[i].toString();
                if (board.getEntryTeam(dummyKey) != null) board.resetScores(dummyKey);
                stale.unregister();
            }
        }
    }
}
