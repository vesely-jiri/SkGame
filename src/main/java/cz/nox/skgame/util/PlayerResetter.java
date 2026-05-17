package cz.nox.skgame.util;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

public final class PlayerResetter {

    private PlayerResetter() {}

    public static void reset(Player player, GameMode gameMode) {
        player.setFireTicks(0);
        player.getActivePotionEffects().forEach(e -> player.removePotionEffect(e.getType()));
        player.leaveVehicle();
        player.getInventory().clear();
        player.resetMaxHealth();
        player.setHealth(player.getMaxHealth());
        player.setExp(0f);
        player.setLevel(0);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.setInvulnerable(false);
        player.setFoodLevel(20);
        player.setSaturation(5.0f);
        player.setGameMode(gameMode);
        player.resetTitle();
    }
}
