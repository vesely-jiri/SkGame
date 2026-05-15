package cz.nox.skgame.core.admin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class PositionIndicator {

    private final Location location;
    private final Plugin plugin;
    private BukkitTask task;
    private BlockData originalData;

    public PositionIndicator(Location location, Plugin plugin) {
        this.location = location.clone();
        this.plugin = plugin;
    }

    public void start() {
        originalData = location.getBlock().getBlockData();
        task = new BukkitRunnable() {
            boolean blue = false;
            @Override
            public void run() {
                blue = !blue;
                if (blue) {
                    location.getBlock().setType(Material.BLUE_CONCRETE, false);
                } else {
                    location.getBlock().setBlockData(originalData, false);
                }
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (originalData != null) {
            location.getBlock().setBlockData(originalData, false);
            originalData = null;
        }
    }
}
