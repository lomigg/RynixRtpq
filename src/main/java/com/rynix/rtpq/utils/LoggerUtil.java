package com.rynix.rtpq.utils;

import org.bukkit.Bukkit;
import java.util.logging.Logger;

/**
 * Logger Utility - Premium Fancy Log
 * Plugin: RynixRtpq
 * Author: RinZz - Rynix Studio
 */
public final class LoggerUtil {
    private static final String PREFIX = "[RynixRtpq] ";
    private static Logger logger;
    private static final String VERSION = "2.1.0-JAVA21-25-DUO";

    private LoggerUtil() {}

    public static void init(Logger bukkitLogger) {
        logger = bukkitLogger;
    }

    public static void info(String msg) {
        if (logger != null) logger.info(PREFIX + msg);
        else Bukkit.getLogger().info(PREFIX + msg);
    }

    public static void warn(String msg) {
        if (logger != null) logger.warning(PREFIX + msg);
        else Bukkit.getLogger().warning(PREFIX + msg);
    }

    public static void error(String msg) {
        if (logger != null) logger.severe(PREFIX + msg);
        else Bukkit.getLogger().severe(PREFIX + msg);
    }

    public static void debug(String msg, boolean debugEnabled) {
        if (debugEnabled) info("[DEBUG] " + msg);
    }

    /**
     * Fancy startup banner - Author RinZz - Xịn
     */
    public static void logStartup() {
        // Use Bukkit console sender with colors
        var console = Bukkit.getConsoleSender();
        console.sendMessage("");
        console.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        console.sendMessage("");
        console.sendMessage("§b  ██████╗ ██╗   ██╗███╗   ██╗██╗██╗  ██╗");
        console.sendMessage("§b  ██╔══██╗╚██╗ ██╔╝████╗  ██║██║╚██╗██╔╝");
        console.sendMessage("§b  ██████╔╝ ╚████╔╝ ██╔██╗ ██║██║ ╚███╔╝ ");
        console.sendMessage("§3  ██╔══██╗  ╚██╔╝  ██║╚██╗██║██║ ██╔██╗ ");
        console.sendMessage("§3  ██║  ██║   ██║   ██║ ╚████║██║██╔╝ ██╗");
        console.sendMessage("§3  ╚═╝  ╚═╝   ╚═╝   ╚═╝  ╚═══╚═╝╚═╝  ╚═╝");
        console.sendMessage("");
        console.sendMessage("§f   ██████╗ ████████╗██████╗  ██████╗ ");
        console.sendMessage("§f   ██╔══██╗╚══██╔══╝██╔══██╗██╔═══██╗");
        console.sendMessage("§f   ██████╔╝   ██║   ██████╔╝██║   ██║");
        console.sendMessage("§7   ██╔══██╗   ██║   ██╔═══╝ ██║▄▄ ██║");
        console.sendMessage("§7   ██║  ██║   ██║   ██║     ╚██████╔╝");
        console.sendMessage("§8   ╚═╝  ╚═╝   ╚═╝   ╚═╝      ╚══▀▀═╝ ");
        console.sendMessage("");
        console.sendMessage("§8  ┌─ §fThông tin Plugin §8────────────────────────────────┐");
        console.sendMessage("§8  │ §7• §fTên: §bRynixRtpq §8| §7Version: §a" + VERSION);
        console.sendMessage("§8  │ §7• §fAuthor: §bRinZz §8| §7Studio: §dRynix Studio");
        console.sendMessage("§8  │ §7• §fStatus: §a§lENABLED §8| §7Optimized: §a✔ §8| §dDUO");
        console.sendMessage("§8  │ §7• §fFolia: §a✔ Supported §8| §7Paper: §a✔ §8| §7Spigot: §a✔");
        console.sendMessage("§8  │ §7• §fGUI: §eInventory GUI §8| §7Mode: §dDUO 2in1 §8| §7McPlugin Format");
        console.sendMessage("§8  │ §7• §fCommands: §e/rtpq, /rtpqueue §8| §7Both open GUI");
        console.sendMessage("§8  │ §7• §fJava: §a21,22,23,24,25+ §8| §7MC: §a1.8-1.21.5+ §8| §7All Versions");
        console.sendMessage("§8  └───────────────────────────────────────────────────┘");
        console.sendMessage("");
        console.sendMessage("§a  ✓ §fPlugin loaded successfully!");
        console.sendMessage("§b  ♡ §fCảm ơn bạn đã sử dụng plugin của RinZz!");
        console.sendMessage("");
        console.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        console.sendMessage("");

        // Also log to file logger
        if (logger != null) {
            logger.info("=====================================================");
            logger.info(" RYNIX RTPQ v" + VERSION + " - Author: RinZz");
            logger.info(" Premium Optimized & Folia Supported");
            logger.info(" Enabled successfully!");
            logger.info("=====================================================");
        }
    }

    public static void logShutdown() {
        var console = Bukkit.getConsoleSender();
        console.sendMessage("");
        console.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        console.sendMessage("§c  ██████╗ ██╗   ██╗███╗   ██╗██╗██╗  ██╗");
        console.sendMessage("§c  ██╔══██╗╚██╗ ██╔╝████╗  ██║██║╚██╗██╔╝  §7Disabling...");
        console.sendMessage("§4  ██████╔╝ ╚████╔╝ ██╔██╗ ██║██║ ╚███╔╝ ");
        console.sendMessage("");
        console.sendMessage("§7  RynixRtpq §8| §7Author: §bRinZz §8| §cOFF");
        console.sendMessage("§7  Hẹn gặp lại! §c♡");
        console.sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        console.sendMessage("");
    }

    public static void logAuthor() {
        Bukkit.getConsoleSender().sendMessage("§b[RynixRtpq] §fPlugin by §bRinZz §8- §dRynix Studio §8| §7Version " + VERSION);
    }
}
