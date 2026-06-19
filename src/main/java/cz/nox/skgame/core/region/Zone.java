package cz.nox.skgame.core.region;

import org.bukkit.Location;

/**
 * A named sub-region within a game arena. Semantically distinct from the arena region
 * but geometrically identical — both are cuboid regions set via the admin wand.
 * Use "type: a zone" in gamemap value definitions to distinguish zone values from arena.
 */
public final class Zone extends CuboidRegion {

    public Zone(Location pos1, Location pos2) {
        super(pos1, pos2);
    }
}
