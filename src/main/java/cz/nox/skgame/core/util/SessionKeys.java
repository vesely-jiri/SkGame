package cz.nox.skgame.core.util;

public final class SessionKeys {
    private SessionKeys() {}

    /** Prefix for per-team score temp values: "skgame.team.score.<teamId>". Reset at startGame(); cleared with all temp values at runDeferredBlock. */
    public static final String TEAM_SCORE_PREFIX = "skgame.team.score.";
}
