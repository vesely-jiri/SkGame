package cz.nox.skgame.core.storage;

import cz.nox.skgame.SkGame;
import cz.nox.skgame.api.statistics.GameResult;
import cz.nox.skgame.api.statistics.LeaderboardEntry;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class GameResultsRepository {

    private static GameResultsRepository instance;

    private GameResultsRepository() {}

    public static GameResultsRepository getInstance() {
        if (instance == null) instance = new GameResultsRepository();
        return instance;
    }

    /**
     * Enqueues an async task to persist one completed game result.
     * All Player-derived data must be converted to UUIDs/strings before calling (main-thread capture).
     */
    public void recordAsync(SkGame plugin, String sessionId, String minigameId, String gamemapId,
                            long startTime, long endTime, String reason,
                            Set<UUID> playerUuids, Set<UUID> winnerUuids) {
        if (!DatabaseManager.getInstance().isAvailable()) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                insert(sessionId, minigameId, gamemapId, startTime, endTime, reason, playerUuids, winnerUuids);
            } catch (SQLException e) {
                plugin.getLogUtil().warning("Failed to record game result for session " + sessionId + ": " + e.getMessage());
            }
        });
    }

    private void insert(String sessionId, String minigameId, String gamemapId,
                        long startTime, long endTime, String reason,
                        Set<UUID> playerUuids, Set<UUID> winnerUuids) throws SQLException {
        DatabaseManager db = DatabaseManager.getInstance();
        try (Connection conn = db.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long resultId = insertGameResult(conn, sessionId, minigameId, gamemapId,
                        startTime, endTime, reason, winnerUuids);
                insertParticipants(conn, resultId, playerUuids, winnerUuids);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private long insertGameResult(Connection conn, String sessionId, String minigameId, String gamemapId,
                                  long startTime, long endTime, String reason,
                                  Set<UUID> winnerUuids) throws SQLException {
        String winners = winnerUuids.stream().map(UUID::toString)
                .reduce((a, b) -> a + "," + b).orElse("");
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO game_results(session_id,minigame_id,gamemap_id,start_time,end_time,reason,winners) VALUES(?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sessionId);
            ps.setString(2, minigameId);
            ps.setString(3, gamemapId);
            ps.setLong(4, startTime);
            ps.setLong(5, endTime);
            ps.setString(6, reason);
            ps.setString(7, winners);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("No generated key for game_results insert");
            }
        }
    }

    private void insertParticipants(Connection conn, long resultId,
                                    Set<UUID> playerUuids, Set<UUID> winnerUuids) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO game_participants(game_result_id,player_uuid,is_winner) VALUES(?,?,?)")) {
            for (UUID uuid : playerUuids) {
                ps.setLong(1, resultId);
                ps.setString(2, uuid.toString());
                ps.setInt(3, winnerUuids.contains(uuid) ? 1 : 0);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    // ── Query helpers ────────────────────────────────────────────────────────

    public int getWinsByMinigame(UUID playerUuid, String minigameId) {
        if (!DatabaseManager.getInstance().isAvailable()) return 0;
        String sql = """
                SELECT COUNT(*) FROM game_participants p
                JOIN game_results r ON p.game_result_id = r.id
                WHERE p.player_uuid = ? AND r.minigame_id = ? AND p.is_winner = 1""";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, minigameId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    public List<Map<String, Object>> getResultsForPlayer(UUID playerUuid, int limit) {
        if (!DatabaseManager.getInstance().isAvailable()) return List.of();
        String sql = """
                SELECT r.id, r.minigame_id, r.gamemap_id, r.start_time, r.end_time, r.reason,
                       p.is_winner
                FROM game_participants p
                JOIN game_results r ON p.game_result_id = r.id
                WHERE p.player_uuid = ?
                ORDER BY r.end_time DESC
                LIMIT ?""";
        List<Map<String, Object>> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(Map.of(
                            "id",          rs.getLong("id"),
                            "minigame_id", rs.getString("minigame_id"),
                            "gamemap_id",  rs.getString("gamemap_id"),
                            "start_time",  rs.getLong("start_time"),
                            "end_time",    rs.getLong("end_time"),
                            "reason",      rs.getString("reason") != null ? rs.getString("reason") : "",
                            "is_winner",   rs.getInt("is_winner") == 1
                    ));
                }
            }
        } catch (SQLException e) {
            return List.of();
        }
        return results;
    }

    public List<GameResult> getGameResults(UUID playerUuid, @Nullable String minigameId, int limit) {
        if (!DatabaseManager.getInstance().isAvailable()) return List.of();
        String sql = "SELECT r.id, r.minigame_id, r.gamemap_id, r.start_time, r.end_time, r.reason, p.is_winner"
                + " FROM game_participants p JOIN game_results r ON p.game_result_id = r.id"
                + " WHERE p.player_uuid = ?"
                + (minigameId != null ? " AND r.minigame_id = ?" : "")
                + " ORDER BY r.end_time DESC LIMIT ?";
        List<GameResult> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, playerUuid.toString());
            if (minigameId != null) ps.setString(idx++, minigameId);
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new GameResult(
                            rs.getLong("id"),
                            rs.getString("minigame_id"),
                            rs.getString("gamemap_id"),
                            rs.getLong("start_time"),
                            rs.getLong("end_time"),
                            rs.getString("reason") != null ? rs.getString("reason") : "",
                            rs.getInt("is_winner") == 1
                    ));
                }
            }
        } catch (SQLException e) {
            return List.of();
        }
        return results;
    }

    public int getPlayCount(UUID playerUuid, String minigameId) {
        if (!DatabaseManager.getInstance().isAvailable()) return 0;
        String sql = "SELECT COUNT(*) FROM game_participants p"
                + " JOIN game_results r ON p.game_result_id = r.id"
                + " WHERE p.player_uuid = ? AND r.minigame_id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, minigameId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    public List<LeaderboardEntry> getTopPlayersByWins(String minigameId, int limit) {
        String sql = "SELECT p.player_uuid, SUM(p.is_winner) as wins, COUNT(*) as plays"
                + " FROM game_participants p JOIN game_results r ON p.game_result_id = r.id"
                + " WHERE r.minigame_id = ? GROUP BY p.player_uuid ORDER BY wins DESC LIMIT ?";
        return queryLeaderboard(sql, minigameId, limit, -1);
    }

    public List<LeaderboardEntry> getTopPlayersByPlays(String minigameId, int limit) {
        String sql = "SELECT p.player_uuid, SUM(p.is_winner) as wins, COUNT(*) as plays"
                + " FROM game_participants p JOIN game_results r ON p.game_result_id = r.id"
                + " WHERE r.minigame_id = ? GROUP BY p.player_uuid ORDER BY plays DESC LIMIT ?";
        return queryLeaderboard(sql, minigameId, limit, -1);
    }

    public List<LeaderboardEntry> getTopPlayersByWinRate(String minigameId, int limit, int minPlays) {
        String sql = "SELECT p.player_uuid, SUM(p.is_winner) as wins, COUNT(*) as plays"
                + " FROM game_participants p JOIN game_results r ON p.game_result_id = r.id"
                + " WHERE r.minigame_id = ? GROUP BY p.player_uuid HAVING plays >= ?"
                + " ORDER BY CAST(SUM(p.is_winner) AS REAL) / COUNT(*) DESC LIMIT ?";
        return queryLeaderboard(sql, minigameId, limit, minPlays);
    }

    // ── Player-profile queries ────────────────────────────────────────────────

    public int getTotalGames(UUID playerUuid) {
        if (!DatabaseManager.getInstance().isAvailable()) return 0;
        String sql = "SELECT COUNT(*) FROM game_participants WHERE player_uuid = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        } catch (SQLException e) { return 0; }
    }

    public int getTotalWins(UUID playerUuid) {
        if (!DatabaseManager.getInstance().isAvailable()) return 0;
        String sql = "SELECT COALESCE(SUM(is_winner), 0) FROM game_participants WHERE player_uuid = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        } catch (SQLException e) { return 0; }
    }

    @Nullable
    public String getFavoriteMinigameId(UUID playerUuid) {
        if (!DatabaseManager.getInstance().isAvailable()) return null;
        String sql = "SELECT r.minigame_id, COUNT(*) as plays "
                + "FROM game_participants p JOIN game_results r ON p.game_result_id = r.id "
                + "WHERE p.player_uuid = ? GROUP BY r.minigame_id ORDER BY plays DESC LIMIT 1";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("minigame_id") : null;
            }
        } catch (SQLException e) { return null; }
    }

    /** Returns map of minigameId → int[]{wins, plays}, ordered by plays desc. */
    public Map<String, int[]> getStatsByMinigame(UUID playerUuid) {
        if (!DatabaseManager.getInstance().isAvailable()) return Map.of();
        String sql = "SELECT r.minigame_id, COALESCE(SUM(p.is_winner), 0) as wins, COUNT(*) as plays "
                + "FROM game_participants p JOIN game_results r ON p.game_result_id = r.id "
                + "WHERE p.player_uuid = ? GROUP BY r.minigame_id ORDER BY plays DESC";
        Map<String, int[]> result = new LinkedHashMap<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.put(rs.getString("minigame_id"),
                            new int[]{rs.getInt("wins"), rs.getInt("plays")});
                }
            }
        } catch (SQLException e) { /* return partial result */ }
        return result;
    }

    /** Count games the player participated in, optionally filtered by minigame. */
    public int countGamesForPlayer(UUID playerUuid, @Nullable String minigameId) {
        if (!DatabaseManager.getInstance().isAvailable()) return 0;
        String sql = "SELECT COUNT(*) FROM game_participants p"
                + " JOIN game_results r ON p.game_result_id = r.id"
                + " WHERE p.player_uuid = ?"
                + (minigameId != null ? " AND r.minigame_id = ?" : "");
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, playerUuid.toString());
            if (minigameId != null) ps.setString(idx, minigameId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getInt(1) : 0; }
        } catch (SQLException e) { return 0; }
    }

    /**
     * Delete all game_participants rows for the player (optionally filtered by minigame).
     * Then delete orphaned game_results (no participants remaining).
     * Returns the number of participant rows deleted.
     */
    public int deletePlayerStats(UUID playerUuid, @Nullable String minigameId) {
        if (!DatabaseManager.getInstance().isAvailable()) return 0;
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Delete participant rows
                String delPart = minigameId != null
                        ? "DELETE FROM game_participants WHERE player_uuid = ?"
                          + " AND game_result_id IN (SELECT id FROM game_results WHERE minigame_id = ?)"
                        : "DELETE FROM game_participants WHERE player_uuid = ?";
                int deleted;
                try (PreparedStatement ps = conn.prepareStatement(delPart)) {
                    ps.setString(1, playerUuid.toString());
                    if (minigameId != null) ps.setString(2, minigameId);
                    deleted = ps.executeUpdate();
                }
                // Delete orphaned game_results
                try (PreparedStatement ps = conn.prepareStatement(
                        "DELETE FROM game_results WHERE id NOT IN"
                        + " (SELECT DISTINCT game_result_id FROM game_participants)")) {
                    ps.executeUpdate();
                }
                conn.commit();
                return deleted;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return 0;
        }
    }

    private List<LeaderboardEntry> queryLeaderboard(String sql, String minigameId, int limit, int minPlays) {
        if (!DatabaseManager.getInstance().isAvailable()) return List.of();
        List<LeaderboardEntry> entries = new ArrayList<>();
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, minigameId);
            if (minPlays >= 0) ps.setInt(idx++, minPlays);
            ps.setInt(idx, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    String name = Bukkit.getOfflinePlayer(uuid).getName();
                    if (name == null) name = "Unknown (" + uuid.toString().substring(0, 8) + ")";
                    entries.add(new LeaderboardEntry(uuid, name, rs.getInt("wins"), rs.getInt("plays")));
                }
            }
        } catch (SQLException e) {
            return List.of();
        }
        return entries;
    }
}
