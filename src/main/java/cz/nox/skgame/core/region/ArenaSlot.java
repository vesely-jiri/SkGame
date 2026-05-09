package cz.nox.skgame.core.region;

import org.bukkit.Location;
import org.jetbrains.annotations.Nullable;

public class ArenaSlot {
    private final Location pasteOrigin;
    private final boolean temporary;
    @Nullable private String claimedBySessionId;

    public ArenaSlot(Location pasteOrigin, boolean temporary) {
        this.pasteOrigin = pasteOrigin.clone();
        this.temporary = temporary;
    }

    public Location getPasteOrigin() { return pasteOrigin.clone(); }
    public boolean isTemporary() { return temporary; }
    public boolean isFree() { return claimedBySessionId == null; }

    @Nullable
    public String getClaimedBySessionId() { return claimedBySessionId; }

    public void claim(String sessionId) { this.claimedBySessionId = sessionId; }
    public void release() { this.claimedBySessionId = null; }
}
