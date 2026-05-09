package cz.nox.skgame.api.region;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;

public interface Region {
    World getWorld();
    boolean contains(Location location);
    Collection<Player> getPlayers();
    <T extends Entity> Collection<T> getEntities(Class<T> type);
    Collection<Block> getBlocks();
    Location getCenter();
    Location getMin();
    Location getMax();
    /** Never removes players, regardless of type argument. */
    void clearEntities(Class<? extends Entity> type);
}
