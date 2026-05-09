package cz.nox.skgame.core.region;

import cz.nox.skgame.api.region.Region;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Region adapter wrapping a WorldGuard {@code ProtectedRegion}. No compile-time dependency
 * on WorldGuard or WorldEdit — all calls go through cached reflection handles from {@link #init()}.
 * <p>
 * Containment check uses the bounding box (min/max corners), so non-cuboid WG regions
 * are approximated as their axis-aligned bounding box.
 */
public class WorldGuardRegion implements Region {

    private static boolean initialized = false;
    private static Method mGetInstance;
    private static Method mGetPlatform;
    private static Method mGetRegionContainer;
    private static Method mGet;
    private static Method mGetRegion;
    private static Method mGetMinPoint;
    private static Method mGetMaxPoint;
    private static Method mAdapt;
    private static Method mBv3GetX;
    private static Method mBv3GetY;
    private static Method mBv3GetZ;

    private final World world;
    private final String regionId;

    public static boolean init() {
        try {
            Class<?> wgClass = Class.forName("com.sk89q.worldguard.WorldGuard");
            mGetInstance = wgClass.getMethod("getInstance");
            Object wgInstance = mGetInstance.invoke(null);

            mGetPlatform = wgClass.getMethod("getPlatform");
            Object platform = mGetPlatform.invoke(wgInstance);

            mGetRegionContainer = platform.getClass().getMethod("getRegionContainer");
            Object rc = mGetRegionContainer.invoke(platform);

            Class<?> weWorldClass = Class.forName("com.sk89q.worldedit.world.World");
            mGet = rc.getClass().getMethod("get", weWorldClass);

            Class<?> rmClass = Class.forName("com.sk89q.worldguard.protection.managers.RegionManager");
            mGetRegion = rmClass.getMethod("getRegion", String.class);

            Class<?> prClass = Class.forName("com.sk89q.worldguard.protection.regions.ProtectedRegion");
            mGetMinPoint = prClass.getMethod("getMinimumPoint");
            mGetMaxPoint = prClass.getMethod("getMaximumPoint");

            Class<?> bv3Class = Class.forName("com.sk89q.worldedit.math.BlockVector3");
            mBv3GetX = bv3Class.getMethod("getX");
            mBv3GetY = bv3Class.getMethod("getY");
            mBv3GetZ = bv3Class.getMethod("getZ");

            Class<?> baClass = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter");
            mAdapt = baClass.getMethod("adapt", World.class);

            initialized = true;
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public WorldGuardRegion(World world, String regionId) {
        this.world = world;
        this.regionId = regionId;
    }

    public String getRegionId() {
        return regionId;
    }

    @Nullable
    private Object getProtectedRegion() {
        try {
            Object wgInstance = mGetInstance.invoke(null);
            Object platform   = mGetPlatform.invoke(wgInstance);
            Object rc         = mGetRegionContainer.invoke(platform);
            Object weWorld    = mAdapt.invoke(null, world);
            Object rm         = mGet.invoke(rc, weWorld);
            if (rm == null) return null;
            return mGetRegion.invoke(rm, regionId);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public World getWorld() {
        return world;
    }

    @Override
    public boolean contains(Location location) {
        if (!world.equals(location.getWorld())) return false;
        Object pr = getProtectedRegion();
        if (pr == null) return false;
        try {
            Object min = mGetMinPoint.invoke(pr);
            Object max = mGetMaxPoint.invoke(pr);
            int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
            return x >= (int) mBv3GetX.invoke(min) && x <= (int) mBv3GetX.invoke(max)
                && y >= (int) mBv3GetY.invoke(min) && y <= (int) mBv3GetY.invoke(max)
                && z >= (int) mBv3GetZ.invoke(min) && z <= (int) mBv3GetZ.invoke(max);
        } catch (Exception e) {
            return false;
        }
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
        Object pr = getProtectedRegion();
        if (pr == null) return Collections.emptyList();
        try {
            Object min = mGetMinPoint.invoke(pr);
            Object max = mGetMaxPoint.invoke(pr);
            int minX = (int) mBv3GetX.invoke(min), minY = (int) mBv3GetY.invoke(min), minZ = (int) mBv3GetZ.invoke(min);
            int maxX = (int) mBv3GetX.invoke(max), maxY = (int) mBv3GetY.invoke(max), maxZ = (int) mBv3GetZ.invoke(max);
            List<Block> blocks = new ArrayList<>((maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1));
            for (int x = minX; x <= maxX; x++)
                for (int y = minY; y <= maxY; y++)
                    for (int z = minZ; z <= maxZ; z++)
                        blocks.add(world.getBlockAt(x, y, z));
            return blocks;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Location getCenter() {
        Object pr = getProtectedRegion();
        if (pr == null) return null;
        try {
            Object min = mGetMinPoint.invoke(pr);
            Object max = mGetMaxPoint.invoke(pr);
            return new Location(world,
                    ((int) mBv3GetX.invoke(min) + (int) mBv3GetX.invoke(max)) / 2.0,
                    ((int) mBv3GetY.invoke(min) + (int) mBv3GetY.invoke(max)) / 2.0,
                    ((int) mBv3GetZ.invoke(min) + (int) mBv3GetZ.invoke(max)) / 2.0);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Location getMin() {
        Object pr = getProtectedRegion();
        if (pr == null) return null;
        try {
            Object min = mGetMinPoint.invoke(pr);
            return new Location(world, (int) mBv3GetX.invoke(min), (int) mBv3GetY.invoke(min), (int) mBv3GetZ.invoke(min));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Location getMax() {
        Object pr = getProtectedRegion();
        if (pr == null) return null;
        try {
            Object max = mGetMaxPoint.invoke(pr);
            return new Location(world, (int) mBv3GetX.invoke(max), (int) mBv3GetY.invoke(max), (int) mBv3GetZ.invoke(max));
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void clearEntities(Class<? extends Entity> type) {
        getEntities(type).stream()
                .filter(e -> !(e instanceof Player))
                .forEach(Entity::remove);
    }
}
