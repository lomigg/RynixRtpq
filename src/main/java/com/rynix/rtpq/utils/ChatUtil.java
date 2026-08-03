package com.rynix.rtpq.utils;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Chat Utility - Optimized color handling
 * Plugin: RynixRtpq | Author: RinZz
 */
public final class ChatUtil {

    private static String prefix = "&8[&bRynixRtpq&8] &f";

    private ChatUtil() {}

    public static void setPrefix(String p) {
        prefix = p;
    }

    public static String color(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public static void send(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(color(prefix + message));
    }

    public static void sendRaw(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(color(message));
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null) return;
        try {
            player.sendActionBar(color(message));
        } catch (NoSuchMethodError e) {
            try {
                Object craftPlayer = player.getClass().getMethod("spigot").invoke(player);
                Class<?> chatMessageType = Class.forName("net.md_5.bungee.api.ChatMessageType");
                Class<?> textComponent = Class.forName("net.md_5.bungee.api.chat.TextComponent");
                Object[] types = chatMessageType.getEnumConstants();
                Object actionBar = types.length > 1 ? types[1] : types[0];
                java.lang.reflect.Method sendMethod = craftPlayer.getClass().getMethod("sendMessage", chatMessageType, Class.forName("net.md_5.bungee.api.chat.BaseComponent"));
                Object component = textComponent.getConstructor(String.class).newInstance(color(message));
                sendMethod.invoke(craftPlayer, actionBar, component);
            } catch (Exception ignored) {}
        }
    }

    public static String formatTime(long seconds) {
        if (seconds < 60) return seconds + "s";
        long m = seconds / 60;
        long s = seconds % 60;
        if (s == 0) return m + "m";
        return m + "m " + s + "s";
    }
}
