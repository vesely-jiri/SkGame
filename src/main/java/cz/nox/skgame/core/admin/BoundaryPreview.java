package cz.nox.skgame.core.admin;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

/**
 * Renders per-admin particle outlines for wand corner selection:
 *   - p1 always: green unit cube (1×1×1 block outline)
 *   - p2 set:     red unit cube at p2 + lime bounding-box across both corners
 * Runs every 5 ticks; ~150 particle cap on the bounding box.
 */
public class BoundaryPreview {

    private static final Particle.DustOptions DUST_GREEN = new Particle.DustOptions(Color.fromRGB(0, 220, 0), 1.5f);
    private static final Particle.DustOptions DUST_RED   = new Particle.DustOptions(Color.fromRGB(255, 60, 60), 1.5f);
    private static final Particle.DustOptions DUST_LIME  = new Particle.DustOptions(Color.fromRGB(50, 255, 50), 1.5f);

    private final Player admin;
    private final Location p1;
    @Nullable private final Location p2;
    private final Plugin plugin;
    private final int density;
    private BukkitTask task;

    public BoundaryPreview(Player admin, Location p1, @Nullable Location p2, Plugin plugin, int density) {
        this.admin = admin;
        this.p1 = p1.clone();
        this.p2 = p2 != null ? p2.clone() : null;
        this.plugin = plugin;
        this.density = Math.max(1, density);
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
        if (world == null || !world.equals(admin.getWorld())) return;

        double unitStep = 1.0 / density;

        // Always: green unit cube at p1
        spawnUnitCube(p1, DUST_GREEN, unitStep);

        if (p2 == null) return;
        if (!world.equals(p2.getWorld())) return;

        // Red unit cube at p2
        spawnUnitCube(p2, DUST_RED, unitStep);

        // Lime bounding box across both corners
        double minX = Math.min(p1.getX(), p2.getX());
        double minY = Math.min(p1.getY(), p2.getY());
        double minZ = Math.min(p1.getZ(), p2.getZ());
        double maxX = Math.max(p1.getX(), p2.getX()) + 1;
        double maxY = Math.max(p1.getY(), p2.getY()) + 1;
        double maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;

        double dx = maxX - minX, dy = maxY - minY, dz = maxZ - minZ;
        // Cap bounding box at density*75 particles total; minimum step = 1/density.
        double step = Math.max(1.0 / density, (dx + dy + dz) / (density * 75.0));

        // Bottom face
        spawnEdgeX(minX, maxX, minY, minZ, step, DUST_LIME);
        spawnEdgeX(minX, maxX, minY, maxZ, step, DUST_LIME);
        spawnEdgeZ(minZ, maxZ, minX, minY, step, DUST_LIME);
        spawnEdgeZ(minZ, maxZ, maxX, minY, step, DUST_LIME);
        // Top face
        spawnEdgeX(minX, maxX, maxY, minZ, step, DUST_LIME);
        spawnEdgeX(minX, maxX, maxY, maxZ, step, DUST_LIME);
        spawnEdgeZ(minZ, maxZ, minX, maxY, step, DUST_LIME);
        spawnEdgeZ(minZ, maxZ, maxX, maxY, step, DUST_LIME);
        // Vertical edges
        spawnEdgeY(minY, maxY, minX, minZ, step, DUST_LIME);
        spawnEdgeY(minY, maxY, maxX, minZ, step, DUST_LIME);
        spawnEdgeY(minY, maxY, minX, maxZ, step, DUST_LIME);
        spawnEdgeY(minY, maxY, maxX, maxZ, step, DUST_LIME);
    }

    private void spawnUnitCube(Location corner, Particle.DustOptions dust, double step) {
        double x0 = corner.getBlockX(), y0 = corner.getBlockY(), z0 = corner.getBlockZ();
        double x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;
        spawnEdgeX(x0, x1, y0, z0, step, dust);
        spawnEdgeX(x0, x1, y0, z1, step, dust);
        spawnEdgeX(x0, x1, y1, z0, step, dust);
        spawnEdgeX(x0, x1, y1, z1, step, dust);
        spawnEdgeZ(z0, z1, x0, y0, step, dust);
        spawnEdgeZ(z0, z1, x0, y1, step, dust);
        spawnEdgeZ(z0, z1, x1, y0, step, dust);
        spawnEdgeZ(z0, z1, x1, y1, step, dust);
        spawnEdgeY(y0, y1, x0, z0, step, dust);
        spawnEdgeY(y0, y1, x1, z0, step, dust);
        spawnEdgeY(y0, y1, x0, z1, step, dust);
        spawnEdgeY(y0, y1, x1, z1, step, dust);
    }

    private void spawnEdgeX(double x0, double x1, double y, double z, double step, Particle.DustOptions dust) {
        for (double x = x0; x <= x1; x += step)
            admin.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, dust);
    }

    private void spawnEdgeY(double y0, double y1, double x, double z, double step, Particle.DustOptions dust) {
        for (double y = y0; y <= y1; y += step)
            admin.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, dust);
    }

    private void spawnEdgeZ(double z0, double z1, double x, double y, double step, Particle.DustOptions dust) {
        for (double z = z0; z <= z1; z += step)
            admin.spawnParticle(Particle.DUST, x, y, z, 1, 0, 0, 0, 0, dust);
    }
}
