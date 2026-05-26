package cz.nox.skgame.api.game.model;

public enum MinigameTag {
    PVP, PVE, FFA, TEAM, BUILDING, PUZZLE, RACE;

    public String displayName() {
        return switch (this) {
            case PVP      -> "PVP";
            case PVE      -> "PVE";
            case FFA      -> "FFA";
            case TEAM     -> "Team";
            case BUILDING -> "Building";
            case PUZZLE   -> "Puzzle";
            case RACE     -> "Race";
        };
    }
}
