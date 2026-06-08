package cz.nox.skgame.core.admin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class LocationBeam {

    private final Location base;
    private final int durationTicks;
    private final Plugin plugin;

    public LocationBeam(Location base, int durationTicks, Plugin plugin) {
        this.base = base.clone();
        this.durationTicks = durationTicks;
        this.plugin = plugin;
    }

    public void spawn() {
        if (base.getWorld() == null) return;

        Location spawnLoc = base.clone();
        spawnLoc.setPitch(0);
        ItemDisplay display = (ItemDisplay) base.getWorld().spawnEntity(spawnLoc, EntityType.ITEM_DISPLAY);
        display.setItemStack(new ItemStack(Material.RED_CONCRETE));
        display.setGlowing(true);
        display.setInterpolationDelay(1);
        display.setTeleportDuration(1);
        display.setTransformation(new Transformation(
                new Vector3f(0f, 0f, 0f),
                new AxisAngle4f(0f, 0f, 1f, 0f),
                new Vector3f(0.25f, 25f, 0.25f),
                new AxisAngle4f(0f, 0f, 1f, 0f)
        ));

        Quaternionf rotStep = new Quaternionf().rotateY((float) Math.toRadians(1.0));

        new BukkitRunnable() {
            private int tick = 0;
            @Override
            public void run() {
                if (tick >= durationTicks || !display.isValid()) {
                    display.remove();
                    cancel();
                    return;
                }
                Transformation t = display.getTransformation();
                Quaternionf newRot = new Quaternionf(t.getLeftRotation()).mul(rotStep);
                display.setTransformation(new Transformation(
                        t.getTranslation(), newRot, t.getScale(), t.getRightRotation()
                ));
                tick++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
