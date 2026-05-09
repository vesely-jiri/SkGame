package cz.nox.skgame.core.region;

import cz.nox.skgame.api.region.Region;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Region adapter wrapping a SkBee {@code Bound}. No compile-time dependency on SkBee —
 * all calls go through cached reflection handles initialised in {@link #init()}.
 */
public class SkBeeBoundRegion implements Region {

    private static Class<?> boundClass;
    private static Method mIsInRegion;
    private static Method mGetEntities;
    private static Method mGetBlocks;
    private static Method mGetWorld;
    private static Method mGetCenter;
    private static Method mGetGreaterCorner;
    private static Method mGetLesserCorner;
    private static Method mGetId;
    private static Method mGetBoundConfig;
    private static Method mGetBoundFromID;

    private final Object bound;

    public static boolean init() {
        try {
            boundClass = Class.forName("com.shanebeestudios.skbee.api.bound.Bound");
            mIsInRegion        = boundClass.getMethod("isInRegion", Location.class);
            mGetEntities       = boundClass.getMethod("getEntities", Class.class);
            mGetBlocks         = boundClass.getMethod("getBlocks");
            mGetWorld          = boundClass.getMethod("getWorld");
            mGetCenter         = boundClass.getMethod("getCenter");
            mGetGreaterCorner  = boundClass.getMethod("getGreaterCorner");
            mGetLesserCorner   = boundClass.getMethod("getLesserCorner");
            mGetId             = boundClass.getMethod("getId");

            Class<?> skBeeClass      = Class.forName("com.shanebeestudios.skbee.SkBee");
            Class<?> boundConfigClass = Class.forName("com.shanebeestudios.skbee.api.bound.BoundConfig");
            mGetBoundConfig  = skBeeClass.getMethod("getBoundConfig");
            mGetBoundFromID  = boundConfigClass.getMethod("getBoundFromID", String.class);
            return true;
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    @Nullable
    public static Class<?> getBoundClass() {
        return boundClass;
    }

    @Nullable
    public static SkBeeBoundRegion fromId(String id) {
        try {
            Object skBee = Bukkit.getPluginManager().getPlugin("SkBee");
            if (skBee == null) return null;
            Object config = mGetBoundConfig.invoke(skBee);
            Object bound  = mGetBoundFromID.invoke(config, id);
            if (bound == null) return null;
            return new SkBeeBoundRegion(bound);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    public SkBeeBoundRegion(Object bound) {
        this.bound = bound;
    }

    public String getId() {
        try {
            return (String) mGetId.invoke(bound);
        } catch (ReflectiveOperationException e) {
            return "";
        }
    }

    @Override
    public World getWorld() {
        try {
            return (World) mGetWorld.invoke(bound);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Override
    public boolean contains(Location location) {
        try {
            return (boolean) mIsInRegion.invoke(bound, location);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }

    @Override
    public Collection<Player> getPlayers() {
        return getEntities(Player.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Collection<T> getEntities(Class<T> type) {
        try {
            return (List<T>) mGetEntities.invoke(bound, type);
        } catch (ReflectiveOperationException e) {
            return Collections.emptyList();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Block> getBlocks() {
        try {
            return (List<Block>) mGetBlocks.invoke(bound);
        } catch (ReflectiveOperationException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Location getCenter() {
        try {
            return (Location) mGetCenter.invoke(bound);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Override
    public Location getMin() {
        try {
            return (Location) mGetLesserCorner.invoke(bound);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    @Override
    public Location getMax() {
        try {
            return (Location) mGetGreaterCorner.invoke(bound);
        } catch (ReflectiveOperationException e) {
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
