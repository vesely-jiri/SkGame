package cz.nox.skgame.core.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import cz.nox.skgame.SkGame;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static DatabaseManager instance;

    private @Nullable HikariDataSource dataSource;
    private @Nullable File dbFile;

    private DatabaseManager() {}

    public static DatabaseManager getInstance() {
        if (instance == null) instance = new DatabaseManager();
        return instance;
    }

    public void initialize(SkGame plugin) throws SQLException {
        File storageDir = new File(plugin.getDataFolder(), "storage");
        storageDir.mkdirs();

        String fileName = plugin.getConfig().getString("storage.sqlite.file", "skgame.db");
        File file = new File(storageDir, fileName);
        this.dbFile = file;
        String jdbcUrl = "jdbc:sqlite:" + file.getAbsolutePath();

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        // SQLite supports only one writer at a time; pool size 1 avoids locking contention.
        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(5000);
        config.setPoolName("SkGame-DB");
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("synchronous", "NORMAL");

        try {
            Class<?> conf = Class.forName("org.apache.logging.log4j.core.config.Configurator");
            Class<?> lvl  = Class.forName("org.apache.logging.log4j.Level");
            conf.getMethod("setLevel", String.class, lvl)
                .invoke(null, "com.zaxxer.hikari", lvl.getField("WARN").get(null));
        } catch (Throwable ignored) {}
        dataSource = new HikariDataSource(config);
        createSchema();

        long rowCount = rowCount(plugin);
        plugin.getLogUtil().info("Database initialized: " + rowCount + " game result row(s) in store.");
    }

    private void createSchema() throws SQLException {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(Schema.CREATE_GAME_RESULTS);
            stmt.execute(Schema.CREATE_GAME_PARTICIPANTS);
            stmt.execute(Schema.IDX_GAME_RESULT_MINIGAME);
            stmt.execute(Schema.IDX_PARTICIPANT_PLAYER);
            stmt.execute(Schema.IDX_PARTICIPANT_WINNER);
            // Migration: add score column to existing DBs; silently ignored if already present
            try { stmt.execute(Schema.MIGRATE_ADD_SCORE); } catch (SQLException ignored) {}
        }
    }

    private long rowCount(SkGame plugin) {
        try (Connection conn = getConnection();
             var ps = conn.prepareStatement("SELECT COUNT(*) FROM game_results");
             var rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            plugin.getLogUtil().warning("DB row count check failed: " + e.getMessage());
            return -1L;
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) throw new SQLException("Database not initialized");
        return dataSource.getConnection();
    }

    public boolean isAvailable() {
        return dataSource != null && !dataSource.isClosed();
    }
    public @Nullable com.zaxxer.hikari.HikariPoolMXBean getHikariPoolMXBean() {
        return dataSource != null ? dataSource.getHikariPoolMXBean() : null;
    }

    public @Nullable File getDbFile() {
        return dbFile;
    }

    public long getTotalGameResultCount() {
        if (dataSource == null) return 0L;
        try (Connection conn = getConnection();
             var ps = conn.prepareStatement("SELECT COUNT(*) FROM game_results");
             var rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        } catch (SQLException e) {
            return -1L;
        }
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
        dataSource = null;
    }
}
