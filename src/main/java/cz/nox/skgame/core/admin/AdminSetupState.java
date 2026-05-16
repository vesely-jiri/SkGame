package cz.nox.skgame.core.admin;

import org.bukkit.Location;

public class AdminSetupState {

    public enum ResponseMode { NONE, MAP_CREATION, VALUE_INPUT, REGION_INPUT }

    private Location pos1;
    private Location pos2;
    private PositionIndicator indicator1;
    private PositionIndicator indicator2;

    private ResponseMode responseMode = ResponseMode.NONE;
    private String currentMapId;
    private String currentMiniGameId;
    private String currentValueKey;

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public boolean hasRegion() { return pos1 != null && pos2 != null; }

    public void setPos1(Location loc, PositionIndicator indicator) {
        if (indicator1 != null) indicator1.stop();
        pos1 = loc;
        indicator1 = indicator;
    }

    public void setPos2(Location loc, PositionIndicator indicator) {
        if (indicator2 != null) indicator2.stop();
        pos2 = loc;
        indicator2 = indicator;
    }

    public void clearPositions() {
        if (indicator1 != null) { indicator1.stop(); indicator1 = null; }
        if (indicator2 != null) { indicator2.stop(); indicator2 = null; }
        pos1 = null;
        pos2 = null;
    }

    public ResponseMode getResponseMode() { return responseMode; }
    public void setResponseMode(ResponseMode mode) { this.responseMode = mode; }

    public String getCurrentMapId() { return currentMapId; }
    public void setCurrentMapId(String id) { this.currentMapId = id; }

    public String getCurrentMiniGameId() { return currentMiniGameId; }
    public void setCurrentMiniGameId(String id) { this.currentMiniGameId = id; }

    public String getCurrentValueKey() { return currentValueKey; }
    public void setCurrentValueKey(String key) { this.currentValueKey = key; }
}
