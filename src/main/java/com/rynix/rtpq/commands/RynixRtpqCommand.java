package com.rynix.rtpq.commands;

import com.rynix.rtpq.RynixRtpqPlugin;
import com.rynix.rtpq.utils.ChatUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * RTPQueue Command - /rtpqueue - Only opens GUI + admin commands
 * User request: rtpq rtpqueue là mở gui, /rtpq invite không cần chỉ cần click trong gui
 * Plugin: RynixRtpq v2.0 | Author: RinZz
 */
public class RynixRtpqCommand implements CommandExecutor, TabCompleter {

    private final RynixRtpqPlugin plugin;

    public RynixRtpqCommand(RynixRtpqPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sendHelp(sender);
                return true;
            }
            // Open GUI as per config
            plugin.getGui().openGUI(player);
            plugin.getSoundManager().playCommandSound(player);
            return true;
        }

        String sub = args[0].toLowerCase();

        // GUI related - all open GUI
        switch (sub) {
            case "gui":
            case "menu":
            case "open":
                if (!(sender instanceof Player player)) {
                    ChatUtil.sendRaw(sender, "&cPlayer only!");
                    return true;
                }
                plugin.getGui().openGUI(player);
                plugin.getSoundManager().playCommandSound(player);
                break;

            case "accept":
                if (!(sender instanceof Player player)) return true;
                if (args.length < 2) {
                    ChatUtil.send(player, "&cBạn cần chỉ rõ tên người mời! Click trong tin nhắn mời để chấp nhận.");
                } else {
                    plugin.getInviteManager().acceptInvite(player, args[1]);
                }
                break;

            case "reload":
                if (!sender.hasPermission("rynixrtpq.admin") && !sender.hasPermission("rtpqueue.admin") && !sender.hasPermission("rtpq.admin")) {
                    ChatUtil.sendRaw(sender, "&cNo permission!");
                    return true;
                }
                plugin.getConfigManager().load();
                ChatUtil.setPrefix(plugin.getConfigManager().getPrefix());
                ChatUtil.send(sender, plugin.getConfigManager().getMessage("reload"));
                if (sender instanceof Player p) plugin.getSoundManager().playSound(p, "success");
                break;

            case "clear":
                if (!sender.hasPermission("rynixrtpq.admin") && !sender.hasPermission("rtpqueue.admin") && !sender.hasPermission("rtpq.admin")) {
                    ChatUtil.sendRaw(sender, "&cNo permission!");
                    return true;
                }
                int count = plugin.getQueueManager().getTotalQueueSize();
                plugin.getQueueManager().clearQueue();
                ChatUtil.send(sender, "&aĐã xóa &e" + count + " &angười trong queue | RynixRtpq");
                break;

            case "list":
            case "stats":
            case "duo":
            case "info":
                sendStats(sender);
                break;

            default:
                // Even if they type world name, just open GUI (since GUI has world selector)
                if (sender instanceof Player player) {
                    plugin.getGui().openGUI(player);
                } else {
                    sendHelp(sender);
                }
                break;
        }
        return true;
    }

    private void sendStats(CommandSender sender) {
        ChatUtil.sendRaw(sender, "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatUtil.sendRaw(sender, "&b&lRynixRtpq &d&lDUO &8v2.0 &7GUI Mode &8| &bRinZz");
        ChatUtil.sendRaw(sender, "&7Config bạn gửi đã có GUI sẵn");
        ChatUtil.sendRaw(sender, "&e/rtpq &7và &e/rtpqueue &7đều mở GUI");
        ChatUtil.sendRaw(sender, "&7Click trong GUI để Join/Leave/World/Invite");
        ChatUtil.sendRaw(sender, "&7Không cần gõ &c/rtpq invite &7- chỉ cần click NETHER_STAR trong GUI");
        ChatUtil.sendRaw(sender, "&7Queue: &e" + plugin.getQueueManager().getTotalQueueSize() + "&7/&e" + plugin.getConfigManager().getMaxQueueSize());
        ChatUtil.sendRaw(sender, "&7Author: &bRinZz &8- &dRynix Studio");
        ChatUtil.sendRaw(sender, "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    private void sendHelp(CommandSender sender) {
        ChatUtil.sendRaw(sender, "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatUtil.sendRaw(sender, "§b  RynixRtpq v2.0 - GUI Mode");
        ChatUtil.sendRaw(sender, "§7  Config có GUI sẵn, rtpq rtpqueue mở GUI");
        ChatUtil.sendRaw(sender, "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        ChatUtil.sendRaw(sender, "&e/rtpq &7- Mở GUI Duo Queue (chính)");
        ChatUtil.sendRaw(sender, "&e/rtpqueue &7- Mở GUI Duo Queue");
        ChatUtil.sendRaw(sender, "&7Trong GUI:");
        ChatUtil.sendRaw(sender, "&a - GREEN: Join queue");
        ChatUtil.sendRaw(sender, "&c - RED: Leave queue");
        ChatUtil.sendRaw(sender, "&a - GRASS_BLOCK: Chọn world");
        ChatUtil.sendRaw(sender, "&b - BOOK: Xem queue {count}");
        ChatUtil.sendRaw(sender, "&e - NETHER_STAR: Mở Invite Menu -> Click đầu player để mời duo");
        ChatUtil.sendRaw(sender, "&7Không cần lệnh invite, chỉ click trong GUI!");
        ChatUtil.sendRaw(sender, "&7Author: &bRinZz | Rynix Studio");
        ChatUtil.sendRaw(sender, "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> subs = Arrays.asList("gui", "reload", "clear", "list", "stats", "accept");
        if (args.length == 1) {
            List<String> result = new ArrayList<>();
            String input = args[0].toLowerCase();
            for (String s : subs) if (s.startsWith(input)) result.add(s);
            return result;
        }
        return new ArrayList<>();
    }
}
