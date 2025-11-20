package me.clin.kOTB.listener;

import me.clin.kOTB.manager.PlayerFreezeManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;

public class FreezeListener implements Listener {

    private final PlayerFreezeManager freezeManager;

    public FreezeListener(PlayerFreezeManager freezeManager) {
        this.freezeManager = freezeManager;
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!freezeManager.isFrozen(player.getUniqueId())) {
            return;
        }
        if (event.getTo() != null && event.getFrom().distanceSquared(event.getTo()) > 0) {
            event.setTo(event.getFrom());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (freezeManager.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (freezeManager.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (freezeManager.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (freezeManager.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (freezeManager.isFrozen(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (freezeManager.isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player damager && freezeManager.isFrozen(damager.getUniqueId())) {
            event.setCancelled(true);
            return;
        }
        if (event.getEntity() instanceof Player player && freezeManager.isFrozen(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
