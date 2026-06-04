package cz.nox.skgame.core.game;

import cz.nox.skgame.api.game.model.MiniGame;
import cz.nox.skgame.api.game.model.Session;
import cz.nox.skgame.api.game.model.type.CancellableEventType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class GameEventCancelListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Set<CancellableEventType> cancelled = getCancelledEvents(player);
        if (cancelled == null) return;

        boolean shouldCancel = cancelled.contains(CancellableEventType.DAMAGE);
        if (!shouldCancel && event instanceof EntityDamageByEntityEvent pvpEv
                && pvpEv.getDamager() instanceof Player) {
            shouldCancel = cancelled.contains(CancellableEventType.PVP);
        }
        if (!shouldCancel && event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            shouldCancel = cancelled.contains(CancellableEventType.FALL_DAMAGE);
        }
        if (shouldCancel) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onHunger(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Set<CancellableEventType> cancelled = getCancelledEvents(player);
        if (cancelled != null && cancelled.contains(CancellableEventType.HUNGER))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemDrop(PlayerDropItemEvent event) {
        Set<CancellableEventType> cancelled = getCancelledEvents(event.getPlayer());
        if (cancelled != null && cancelled.contains(CancellableEventType.ITEM_DROP))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Set<CancellableEventType> cancelled = getCancelledEvents(player);
        if (cancelled != null && cancelled.contains(CancellableEventType.ITEM_PICKUP))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Set<CancellableEventType> cancelled = getCancelledEvents(event.getPlayer());
        if (cancelled != null && cancelled.contains(CancellableEventType.BLOCK_BREAK))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        Set<CancellableEventType> cancelled = getCancelledEvents(event.getPlayer());
        if (cancelled != null && cancelled.contains(CancellableEventType.BLOCK_PLACE))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPortal(PlayerPortalEvent event) {
        Set<CancellableEventType> cancelled = getCancelledEvents(event.getPlayer());
        if (cancelled != null && cancelled.contains(CancellableEventType.PORTAL))
            event.setCancelled(true);
    }

    private @Nullable Set<CancellableEventType> getCancelledEvents(Player player) {
        Session session = SessionManager.getInstance().getSession(player);
        if (session == null) return null;
        MiniGame mg = session.getMiniGame();
        if (mg == null) return null;
        Set<CancellableEventType> cancelled = mg.getCancelledEvents();
        return cancelled.isEmpty() ? null : cancelled;
    }
}
