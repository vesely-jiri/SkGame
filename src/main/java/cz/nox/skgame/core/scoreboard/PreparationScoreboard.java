package cz.nox.skgame.core.scoreboard;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player pre-game sidebar board shown during the PREPARATION and STARTING
 * windows (lobby/selection + start countdown). Minigame-agnostic.
 *
 * <p>Each player gets their own {@link SessionScoreboard} under a shared title
 * and layout, so per-player details (e.g. the team a player picked) can differ
 * while everyone sees the same "Starts in: Xs" countdown. The lifecycle manager
 * creates it on entering PREPARATION/STARTING, updates it each second, and tears
 * it down before the game's own board (if any) takes over at GameStart, or when
 * the session returns to the lobby / disbands.</p>
 *
 * <p>Reuses {@link SessionScoreboard}'s flicker-free team-per-line rendering and
 * its capture/restore of the player's previous board.</p>
 */
public final class PreparationScoreboard {

    private final String sessionId;
    private final ScoreboardConfig config;
    private final Map<UUID, SessionScoreboard> boards = new HashMap<>();

    public PreparationScoreboard(String sessionId, String titleLegacy,
                                 List<String> header, List<String> footer) {
        this.sessionId = sessionId;
        this.config = new ScoreboardConfig(titleLegacy, List.copyOf(header), List.copyOf(footer));
    }

    /**
     * Lazily create and (re)render one player's board.
     * Lines use legacy '&amp;' color codes (translated to section colors by the renderer).
     * No-op for offline/null players.
     */
    public void setPlayerContent(Player player, List<String> legacyLines) {
        if (player == null || !player.isOnline()) return;
        SessionScoreboard board = boards.computeIfAbsent(player.getUniqueId(), u -> {
            SessionScoreboard b = new SessionScoreboard(sessionId, config);
            b.showTo(player);
            return b;
        });
        board.setContent(legacyLines);
    }

    /** Restore a single player's captured board and drop them from this board. */
    public void removePlayer(Player player) {
        if (player == null) return;
        SessionScoreboard board = boards.remove(player.getUniqueId());
        if (board != null) board.dispose();
    }

    /** Restore every shown player's captured board and clear. Safe to call repeatedly. */
    public void dispose() {
        for (SessionScoreboard board : boards.values()) board.dispose();
        boards.clear();
    }
}
