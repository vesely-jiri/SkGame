package cz.nox.skgame.core.region;

import cz.nox.skgame.api.region.Region;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

public class RegionCopier {

    /**
     * Copies all blocks from {@code source} into the world at {@code targetOrigin},
     * preserving relative positions. The source region's min corner maps to targetOrigin.
     * Must be called from the main thread.
     */
    public static void copy(Region source, Location targetOrigin) {
        Location sourceMin = source.getMin();
        if (sourceMin == null) return;
        World targetWorld = targetOrigin.getWorld();
        if (targetWorld == null) return;

        int dx = targetOrigin.getBlockX() - sourceMin.getBlockX();
        int dy = targetOrigin.getBlockY() - sourceMin.getBlockY();
        int dz = targetOrigin.getBlockZ() - sourceMin.getBlockZ();

        for (Block src : source.getBlocks()) {
            Block dest = targetWorld.getBlockAt(src.getX() + dx, src.getY() + dy, src.getZ() + dz);
            dest.setBlockData(src.getBlockData(), false);
        }
    }
}
