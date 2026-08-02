package com.rynix.rtpq.utils;

import com.rynix.rtpq.RynixRtpqPlugin;
import com.rynix.rtpq.config.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * SoundManager - McPlugin style sound system
 * Plugin: RynixRtpq | Author: RinZz
 */
public class SoundManager {

    private final RynixRtpqPlugin plugin;

    public SoundManager(RynixRtpqPlugin plugin) {
        this.plugin = plugin;
    }

    public void playSound(Player player, String... keys) {
        if (player == null) return;
        for (String key : keys) {
            ConfigManager.SoundConfig sc = plugin.getConfigManager().getSound(key);
            if (sc == null) continue;
            try {
                Sound sound = Sound.valueOf(sc.name.toUpperCase());
                player.playSound(player.getLocation(), sound, sc.volume, sc.pitch);
                return;
            } catch (Exception e) {
                try {
                    // Try with legacy parsing
                    Sound sound = Sound.valueOf(sc.name);
                    player.playSound(player.getLocation(), sound, sc.volume, sc.pitch);
                    return;
                } catch (Exception ignored) {}
            }
        }
    }

    public void playCommandSound(Player player) { playSound(player, "command", "click"); }
    public void playJoinSound(Player player) { playSound(player, "join", "success"); }
    public void playLeaveSound(Player player) { playSound(player, "leave", "break"); }
    public void playTeleportSound(Player player) { playSound(player, "teleport", "command"); }
    public void playSuccessSound(Player player) { playSound(player, "success", "join"); }
    public void playClickSound(Player player) { playSound(player, "click", "command"); }
}
