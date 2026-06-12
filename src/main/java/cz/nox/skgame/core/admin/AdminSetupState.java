package cz.nox.skgame.core.admin;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class AdminSetupState {

    public enum ResponseMode { NONE, MAP_CREATION, MAP_RENAME, VALUE_INPUT, REGION_INPUT }

    private Location pos1;
    private Location pos2;
    private BoundaryPreview boundaryPreview;

    private ResponseMode responseMode = ResponseMode.NONE;
    private String currentMapId;
    private String currentMiniGameId;
    private String currentValueKey;

    // ─── Location toggle ──────────────────────────────────────────────────────
    private final List<LocationBeam> activeBeams = new ArrayList<>();
    private boolean locationToggleActive = false;

    public Location getPos1() { return pos1; }
    public Location getPos2() { return pos2; }
    public boolean hasRegion() { return pos1 != null && pos2 != null; }

    public void setPos1(Location loc) { pos1 = loc; }
    public void setPos2(Location loc) { pos2 = loc; }

    public void setBoundaryPreview(BoundaryPreview preview) {
        if (boundaryPreview != null) boundaryPreview.stop();
        boundaryPreview = preview;
    }

    public void clearPositions() {
        if (boundaryPreview != null) { boundaryPreview.stop(); boundaryPreview = null; }
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

    public boolean isLocationToggleActive() { return locationToggleActive; }
    public void setLocationToggleActive(boolean active) { this.locationToggleActive = active; }

    public void addLocationBeam(LocationBeam beam) { activeBeams.add(beam); }

    public void stopAllLocationBeams() {
        activeBeams.forEach(LocationBeam::stop);
        activeBeams.clear();
        locationToggleActive = false;
    }
}
