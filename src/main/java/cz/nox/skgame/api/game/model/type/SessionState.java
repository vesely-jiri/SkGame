package cz.nox.skgame.api.game.model.type;

public enum SessionState {
    STOPPED,
    LOBBY,
    PREPARATION,
    STARTING,
    STARTED,
    /** Post-game celebration/results window. Players still in PLAYER role, arena intact. */
    ENDED
}
