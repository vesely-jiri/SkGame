package cz.nox.skgame.api.statistics;

import java.util.UUID;

public record LeaderboardEntry(UUID playerUuid, String playerName, int wins, int plays) {
    public double winRate() {
        return plays > 0 ? (double) wins / plays : 0.0;
    }
}
