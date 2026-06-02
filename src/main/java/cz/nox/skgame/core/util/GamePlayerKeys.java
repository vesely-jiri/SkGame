package cz.nox.skgame.core.util;

public final class GamePlayerKeys {
    private GamePlayerKeys() {}

    public static final String READY = "ready";
    public static final String JOIN_PARTY_AFTER_GAME = "join_party_after_game";
    /** Reserved temp-value key for per-player score. Reset at startGame(); cleared with all temp values at runDeferredBlock. */
    public static final String SCORE = "skgame.score";
}
