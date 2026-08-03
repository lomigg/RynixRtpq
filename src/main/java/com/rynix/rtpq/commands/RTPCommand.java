package com.rynix.rtpq.commands;

import com.rynix.rtpq.RynixRtpqPlugin;
import com.rynix.rtpq.utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * RTPQ Command - /rtpq - Only opens GUI (as per user request)
 * Config has GUI already, rtpq rtpqueue both open GUI, invite via click in GUI only
 * Plugin: RynixRtpq v2.0 | Author: RinZz
 */
public class RTPCommand implements CommandExecutor, TabCompleter {

    private final RynixRtpqPlugin plugin;

    public RTPCommand(RynixRtpqPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtil.sendRaw(sender, "&cChỉ người chơi mới dùng được!");
            return true;
        }

        if (!player.hasPermission("rynixrtpq.use") && !player.hasPermission("rtpqueue.use") && !player.hasPermission("rtpq.use") && !player.hasPermission("mchashqueue.use")) {
            ChatUtil.sendRaw(player, "&cBạn không có quyền! &8| &bRynixRtpq");
            return true;
        }

        // Always open GUI - as per config you sent, rtpq rtpqueue are GUI openers
        // No need for /rtpq invite command, click in GUI to invite
        if (args.length >= 1 && args[0].equalsIgnoreCase("accept") && args.length >= 2) {
            // Keep accept for clickable invite message
            plugin.getInviteManager().acceptInvite(player, args[1]);
            return true;
        }

        // Open main GUI
        plugin.getGui().openGUI(player);
        plugin.getSoundManager().playCommandSound(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        // No tab needed since only GUI opener, but keep accept
        if (args.length == 1) {
            if ("accept".startsWith(args[0].toLowerCase())) {
                return Collections.singletonList("accept");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("accept")) {
            return null; // Players list
        }
        return Collections.emptyList();
    }
}
