package com.rynix.rtpq.invite;

import com.rynix.rtpq.RynixRtpqPlugin;
import com.rynix.rtpq.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * InviteManager - McPlugin style invite system
 * Plugin: RynixRtpq v2.0 | Author: RinZz
 */
public class InviteManager {

    private final RynixRtpqPlugin plugin;

    // inviter UUID -> set of invited UUIDs with expiry
    private final Map<UUID, Map<UUID, Long>> invites = new ConcurrentHashMap<>();
    // cooldown
    private final Map<UUID, Long> inviteCooldowns = new ConcurrentHashMap<>();

    public InviteManager(RynixRtpqPlugin plugin) {
        this.plugin = plugin;
        startCleanupTask();
    }

    private void startCleanupTask() {
        plugin.getScheduler().runTimer(plugin, this::cleanup, 20L, 100L);
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        for (Map<UUID, Long> map : invites.values()) {
            map.entrySet().removeIf(e -> e.getValue() < now);
        }
        invites.entrySet().removeIf(e -> e.getValue().isEmpty());
        inviteCooldowns.entrySet().removeIf(e -> e.getValue() < now);
    }

    public boolean canInvite(UUID inviter) {
        Long expire = inviteCooldowns.get(inviter);
        if (expire == null) return true;
        return System.currentTimeMillis() > expire;
    }

    public long getCooldownRemaining(UUID inviter) {
        Long expire = inviteCooldowns.get(inviter);
        if (expire == null) return 0;
        long rem = (expire - System.currentTimeMillis()) / 1000;
        return Math.max(0, rem);
    }

    public boolean hasInvite(UUID invited, UUID inviter) {
        Map<UUID, Long> map = invites.get(inviter);
        if (map == null) return false;
        Long expire = map.get(invited);
        if (expire == null) return false;
        if (System.currentTimeMillis() > expire) {
            map.remove(invited);
            return false;
        }
        return true;
    }

    public void sendInvite(Player inviter, Player target) {
        if (inviter == null || target == null) return;

        if (inviter.getUniqueId().equals(target.getUniqueId())) {
            ChatUtil.send(inviter, "&cBạn không thể mời chính mình!");
            return;
        }

        if (!canInvite(inviter.getUniqueId())) {
            long rem = getCooldownRemaining(inviter.getUniqueId());
            String msg = plugin.getConfigManager().getMessage("wait", Map.of("time", String.valueOf(rem)));
            ChatUtil.send(inviter, msg);
            return;
        }

        if (plugin.getQueueManager().isInQueue(target.getUniqueId())) {
            ChatUtil.send(inviter, plugin.getConfigManager().getMessage("already", Map.of()));
            // Already in queue message reused
        }

        if (!plugin.getQueueManager().isInQueue(inviter.getUniqueId())) {
            ChatUtil.send(inviter, plugin.getConfigManager().getMessage("include"));
            return;
        }

        int inviteTime = plugin.getConfigManager().getQueueInviteTime();
        long expire = System.currentTimeMillis() + (inviteTime * 1000L);

        invites.computeIfAbsent(inviter.getUniqueId(), k -> new ConcurrentHashMap<>()).put(target.getUniqueId(), expire);
        inviteCooldowns.put(inviter.getUniqueId(), System.currentTimeMillis() + (plugin.getConfigManager().getQueueInviteCooldown() * 1000L));

        // Send success to inviter
        ChatUtil.send(inviter, plugin.getConfigManager().getMessage("invited", Map.of("friend", target.getName(), "time", String.valueOf(inviteTime))));

        // Send clickable invite to target
        String advEvent = plugin.getConfigManager().getMessage("advevent", Map.of("player", inviter.getName(), "time", String.valueOf(inviteTime)));
        // Replace placeholder for click
        // Try to make clickable via JSON
        try {
            // Fancy clickable message using Bungee API
            String raw = ChatUtil.color(advEvent);
            // Split by [Click Here] if present
            // For simplicity use tellraw via Bukkit dispatch
            // We'll send as raw with click event using Spigot API
            net.md_5.bungee.api.chat.TextComponent comp = new net.md_5.bungee.api.chat.TextComponent(raw.replace("[Click Here]", "").replace("[&d&lClick Here&7]", ""));
            // Actually McPlugin format: "you have been invited by {player}\nto join their queue [Click Here]"
            // We'll make whole message clickable to accept
            net.md_5.bungee.api.chat.TextComponent clickPart = new net.md_5.bungee.api.chat.TextComponent(ChatUtil.color(" &8[&d&lClick Here&8]"));
            clickPart.setClickEvent(new net.md_5.bungee.api.chat.ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/" + plugin.getConfigManager().getQueueHashCommand() + " accept " + inviter.getName()));
            clickPart.setHoverEvent(new net.md_5.bungee.api.chat.HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new net.md_5.bungee.api.chat.text.TextComponent(ChatUtil.color("&aClick để tham gia duo cùng " + inviter.getName()))));

            net.md_5.bungee.api.chat.TextComponent full = new net.md_5.bungee.api.chat.TextComponent("");
            full.addExtra(comp);
            full.addExtra(clickPart);

            target.spigot().sendMessage(full);
        } catch (Exception e) {
            // Fallback
            ChatUtil.sendRaw(target, advEvent + " &8- &7Dùng &e/" + plugin.getConfigManager().getQueueHashCommand() + " accept " + inviter.getName() + " &7để chấp nhận");
        }

        plugin.getSoundManager().playSound(target, "invite", "click");
    }

    public boolean acceptInvite(Player accepter, String inviterName) {
        Player inviter = Bukkit.getPlayer(inviterName);
        if (inviter == null) {
            ChatUtil.send(accepter, plugin.getConfigManager().getMessage("non-player"));
            return false;
        }

        if (!hasInvite(accepter.getUniqueId(), inviter.getUniqueId())) {
            ChatUtil.send(accepter, plugin.getConfigManager().getMessage("time-out"));
            return false;
        }

        // Remove invite
        Map<UUID, Long> map = invites.get(inviter.getUniqueId());
        if (map != null) map.remove(accepter.getUniqueId());

        // Add accepter to queue with same world as inviter
        var inviterQueued = plugin.getQueueManager().getQueuedPlayer(inviter.getUniqueId());
        String world = inviterQueued != null ? inviterQueued.getWorldName() : plugin.getConfigManager().getWorldDefault();

        if (!plugin.getQueueManager().isInQueue(accepter.getUniqueId())) {
            plugin.getQueueManager().addToQueue(accepter, world);
            ChatUtil.send(accepter, plugin.getConfigManager().getMessage("join"));
        }

        ChatUtil.send(inviter, "&a" + accepter.getName() + " &7đã chấp nhận lời mời duo của bạn!");
        return true;
    }

    public void removeInvites(UUID inviter) {
        invites.remove(inviter);
    }
}
