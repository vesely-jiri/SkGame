package cz.nox.skgame.core.storage;

public final class Schema {

    private Schema() {}

    public static final String CREATE_GAME_RESULTS = """
            CREATE TABLE IF NOT EXISTS game_results (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                session_id TEXT    NOT NULL,
                minigame_id TEXT   NOT NULL,
                gamemap_id  TEXT   NOT NULL,
                start_time INTEGER NOT NULL,
                end_time   INTEGER NOT NULL,
                reason     TEXT,
                winners    TEXT
            )""";

    public static final String CREATE_GAME_PARTICIPANTS = """
            CREATE TABLE IF NOT EXISTS game_participants (
                id             INTEGER PRIMARY KEY AUTOINCREMENT,
                game_result_id INTEGER NOT NULL,
                player_uuid    TEXT    NOT NULL,
                is_winner      INTEGER DEFAULT 0,
                FOREIGN KEY (game_result_id) REFERENCES game_results(id)
            )""";

    public static final String IDX_GAME_RESULT_MINIGAME =
            "CREATE INDEX IF NOT EXISTS idx_game_result_minigame ON game_results(minigame_id)";

    public static final String IDX_PARTICIPANT_PLAYER =
            "CREATE INDEX IF NOT EXISTS idx_participant_player ON game_participants(player_uuid)";

    // Composite index for leaderboard queries: wins per player per minigame
    public static final String IDX_PARTICIPANT_WINNER =
            "CREATE INDEX IF NOT EXISTS idx_participant_winner ON game_participants(player_uuid, is_winner)";
}
