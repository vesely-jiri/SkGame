package cz.nox.skgame.core.region;

import cz.nox.skgame.api.region.Region;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CuboidRegion implements Region {

    private final World world;
    private final int minX, minY, minZ;
    private final int maxX, maxY, maxZ;

    public CuboidRegion(Location corner1, Location corner2) {
        this.world = corner1.getWorld();
        this.minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        this.minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        this.minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        this.maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        this.maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        this.maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public boolean contains(Location location) {
        if (location.getWorld() == null || !world.equals(location.getWorld())) return false;
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        // +1 on maxY: player/entity feet are 1 block above the surface block they stand on.
        // Admin wands block coords; include the space players occupy when standing on top.
        return x >= minX && x <= maxX && y >= minY && y <= maxY + 1 && z >= minZ && z <= maxZ;
    }

    @Override
    public Collection<Player> getPlayers() {
        return world.getPlayers().stream()
                .filter(p -> contains(p.getLocation()))
                .collect(Collectors.toList());
    }

    @Override
    public <T extends Entity> Collection<T> getEntities(Class<T> type) {
        return world.getEntitiesByClass(type).stream()
                .filter(e -> contains(e.getLocation()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Block> getBlocks() {
        List<Block> blocks = new ArrayList<>((maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1));
        for (int x = minX; x <= maxX; x++)
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    blocks.add(world.getBlockAt(x, y, z));
        return blocks;
    }

    @Override
    public Location getCenter() {
        return new Location(world,
                (minX + maxX) / 2.0,
                (minY + maxY) / 2.0,
                (minZ + maxZ) / 2.0);
    }

    @Override
    public Location getMin() {
        return new Location(world, minX, minY, minZ);
    }

    @Override
    public Location getMax() {
        return new Location(world, maxX, maxY, maxZ);
    }

    @Override
    public void clearEntities(Class<? extends Entity> type) {
        getEntities(type).stream()
                .filter(e -> !(e instanceof Player))
                .forEach(Entity::remove);
    }

    public int getMinX() { return minX; }
    public int getMinY() { return minY; }
    public int getMinZ() { return minZ; }
    public int getMaxX() { return maxX; }
    public int getMaxY() { return maxY; }
    public int getMaxZ() { return maxZ; }
}
