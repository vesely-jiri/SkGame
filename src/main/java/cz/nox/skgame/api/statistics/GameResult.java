package cz.nox.skgame.api.statistics;

public record GameResult(
        long id,
        String minigameId,
        String gamemapId,
        long startTime,
        long endTime,
        String reason,
        boolean winner
) {}
