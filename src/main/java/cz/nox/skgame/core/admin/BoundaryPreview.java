package cz.nox.skgame.core.admin;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * Renders the 12 edges of the selection bounding box as lime DUST particles,
 * sent only to the admin player every 5 ticks. Particle count is capped at ~150
 * per render cycle regardless of region size via proportional edge sampling.
 */
public class BoundaryPreview {

    private static final Particle.DustOptions DUST = new Particle.DustOptions(Color.fromRGB(50, 255, 50), 1.5f);

    private final Player admin;
    private final Location p1;
    private final Location p2;
    private final Plugin plugin;
    private BukkitTask task;

    public BoundaryPreview(Player admin, Location p1, Location p2, Plugin plugin) {
        this.admin = admin;
        this.p1 = p1.clone();
        this.p2 = p2.clone();
        this.plugin = plugin;
    }

    public void start() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!admin.isOnline()) { cancel(); return; }
                render();
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    public void stop() {
        if (task != null) { task.cancel(); task = null; }
    }

    private void render() {
        World world = p1.getWorld();
        if (world == null || !world.equals(p2.getWorld()) || !world.equals(admin.getWorld())) return;

        double minX = Math.min(p1.getX(), p2.getX());
        double minY = Math.min(p1.getY(), p2.getY());
        double minZ = Math.min(p1.getZ(), p2.getZ());
        double maxX = Math.max(p1.getX(), p2.getX()) + 1;
        double maxY = Math.max(p1.getY(), p2.getY()) + 1;
        double maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;

        double dx = maxX - minX;
        double dy = maxY - minY;
        double dz = maxZ - minZ;
        // Step size proportional to total perimeter — caps total particles at ~150
        double step = Math.max(1.0, 4 * (dx + dy + dz) / 150.0);

        // Bottom face (y = minY)
        spawnEdgeX(minX, maxX, minY, minZ, step);
        spawnEdgeX(minX, maxX, minY, maxZ, step);
        spawnEdgeZ(minZ, maxZ, minX, minY, step);
        spawnEdgeZ(minZ, maxZ, maxX, minY, step);

        // Top face (y = maxY)
        spawnEdgeX(minX, maxX, maxY, minZ, step);
        spawnEdgeX(minX, maxX, maxY, maxZ, step);
        spawnEdgeZ(minZ, maxZ, minX, maxY, step);
        spawnEdgeZ(minZ, maxZ, maxX, maxY, step);

        // Vertical edges
        spawnEdgeY(minY, maxY, minX, minZ, step);
        spawnEdgeY(minY, maxY, maxX, minZ, step);
        spawnEdgeY(minY, maxY, minX, maxZ, step);
        spawnEdgeY(minY, maxY, maxX, maxZ, step);
    }

    private void spawnEdgeX(double x0, double x1, double y, double z, double step) {
        for (double x = x0; x <= x1; x += step)
            admin.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, DUST);
    }

    private void spawnEdgeY(double y0, double y1, double x, double z, double step) {
        for (double y = y0; y <= y1; y += step)
            admin.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, DUST);
    }

    private void spawnEdgeZ(double z0, double z1, double x, double y, double step) {
        for (double z = z0; z <= z1; z += step)
            admin.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, DUST);
    }
}
