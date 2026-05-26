package cz.nox.skgame.api.game.model;

public enum MinigameTag {
    PVP, PVE, FFA, TEAM, BUILDING, PUZZLE, RACE;

    public String displayName() {
        String n = name();
        return n.charAt(0) + n.substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}
