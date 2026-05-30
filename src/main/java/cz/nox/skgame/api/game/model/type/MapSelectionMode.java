package cz.nox.skgame.api.game.model.type;

public enum MapSelectionMode {
    /** A specific map has been chosen by the host. */
    SPECIFIC,
    /** Map decided by player vote during the preparation window. */
    VOTE,
    /** Server picks a random candidate map at game start, no prep window. */
    RANDOM
}
