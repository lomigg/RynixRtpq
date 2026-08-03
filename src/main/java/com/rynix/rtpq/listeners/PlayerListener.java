package com.rynix.rtpq.listeners;

import com.rynix.rtpq.RynixRtpqPlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Player Listener - Handles quit, damage cancel, invulnerability
 * Plugin: RynixRtpq v2.0 | Author: RinZz
 */
public class PlayerListener implements Listener {

    private final RynixRtpqPlugin plugin;

    public PlayerListener(RynixRtpqPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.getQueueManager().handlePlayerQuit(e.getPlayer().getUniqueId());
        plugin.getCooldownManager().cleanup();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;

        // Invulnerability check
        if (plugin.getQueueManager().isInvulnerable(player.getUniqueId())) {
            if (e.getCause() != EntityDamageEvent.DamageCause.VOID) {
                e.setCancelled(true);
                return;
            }
        }

        // Damaged cancel - McPlugin format: if damaged-cancel true and player in queue, cancel queue
        if (plugin.getConfigManager().isDamagedCancel()) {
            if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
                plugin.getQueueManager().handleDamage(player);
            }
        }
    }
}
