package com.rynix.rtpq.utils;

import org.bukkit.Bukkit;

/**
 * Compatibility Utility - Java 21-25 & All Minecraft versions 1.8-1.21.5+
 * Plugin: RynixRtpq | Author: RinZz - Rynix Studio
 */
public final class CompatibilityUtil {

    private CompatibilityUtil() {}

    public static String getJavaVersion() {
        return System.getProperty("java.version", "unknown");
    }

    public static int getJavaMajorVersion() {
        String version = System.getProperty("java.version");
        if (version == null) return -1;
        try {
            if (version.startsWith("1.")) {
                return Integer.parseInt(version.substring(2, 3));
            } else {
                int dot = version.indexOf(".");
                if (dot != -1) {
                    return Integer.parseInt(version.substring(0, dot));
                } else {
                    return Integer.parseInt(version);
                }
            }
        } catch (Exception e) {
            return -1;
        }
    }

    public static boolean isJava21Plus() {
        return getJavaMajorVersion() >= 21;
    }

    public static boolean isJavaSupported() {
        int major = getJavaMajorVersion();
        // Support Java 21,22,23,24,25+ (and also lower for legacy)
        return major >= 8; // Actually allow 8+ but recommend 21+
    }

    public static String getMinecraftVersion() {
        try {
            return Bukkit.getBukkitVersion();
        } catch (Exception e) {
            return Bukkit.getVersion();
        }
    }

    public static String getServerType() {
        try {
            if (Class.forName("io.papermc.paper.threadedregions.RegionizedServer") != null) {
                return "Folia";
            }
        } catch (ClassNotFoundException ignored) {}
        try {
            if (Bukkit.getName().toLowerCase().contains("paper")) return "Paper";
            if (Bukkit.getName().toLowerCase().contains("purpur")) return "Purpur";
            if (Bukkit.getName().toLowerCase().contains("spigot")) return "Spigot";
            if (Bukkit.getName().toLowerCase().contains("bukkit")) return "Bukkit";
        } catch (Exception ignored) {}
        return Bukkit.getName();
    }

    public static void logCompatibility() {
        String javaVer = getJavaVersion();
        int javaMajor = getJavaMajorVersion();
        String mcVer = getMinecraftVersion();
        String serverType = getServerType();

        Bukkit.getConsoleSender().sendMessage("");
        Bukkit.getConsoleSender().sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.getConsoleSender().sendMessage("§b  RynixRtpq Compatibility Check - Author RinZz");
        Bukkit.getConsoleSender().sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.getConsoleSender().sendMessage("§7 Java Version: §a" + javaVer + " §8(Major: " + javaMajor + ")");
        if (javaMajor >= 21 && javaMajor <= 25) {
            Bukkit.getConsoleSender().sendMessage("§a  ✓ Java " + javaMajor + " supported! (21-25) - Optimized");
        } else if (javaMajor >= 17) {
            Bukkit.getConsoleSender().sendMessage("§e  ⚠ Java " + javaMajor + " works but recommended Java 21-25 for best performance");
        } else if (javaMajor >= 8) {
            Bukkit.getConsoleSender().sendMessage("§e  ⚠ Java " + javaMajor + " legacy support - Works but not optimized, use Java 21+");
        } else {
            Bukkit.getConsoleSender().sendMessage("§c  ✗ Java " + javaMajor + " may not be fully supported, please use Java 21-25");
        }
        Bukkit.getConsoleSender().sendMessage("§7 Minecraft Version: §e" + mcVer);
        Bukkit.getConsoleSender().sendMessage("§7 Server Type: §b" + serverType);
        if (serverType.equals("Folia")) {
            Bukkit.getConsoleSender().sendMessage("§a  ✓ Folia detected - Using FoliaScheduler (Regionized Multithreading)");
        } else {
            Bukkit.getConsoleSender().sendMessage("§a  ✓ " + serverType + " detected - Using PaperScheduler (Optimized)");
        }
        Bukkit.getConsoleSender().sendMessage("§7 Supported MC: §a1.8 - 1.21.5+ (All versions)");
        Bukkit.getConsoleSender().sendMessage("§7 Supported Java: §a21,22,23,24,25+ (and legacy 8-20)");
        Bukkit.getConsoleSender().sendMessage("§8§m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        Bukkit.getConsoleSender().sendMessage("");
    }

    public static boolean isModernMinecraft() {
        try {
            // Check if 1.16+ API exists
            Class.forName("org.bukkit.persistence.PersistentDataContainer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
