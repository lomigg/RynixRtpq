package com.rynix.rtpq.gui;

import com.rynix.rtpq.RynixRtpqPlugin;
import com.rynix.rtpq.config.ConfigManager;
import com.rynix.rtpq.queue.QueuedPlayer;
import com.rynix.rtpq.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * Main Menu GUI - McPlugin format compatible
 * Reads menu.* from config: leave, join, world, queue, invite, duo-info
 * Plugin: RynixRtpq v2.0 Duo | Author: RinZz
 */
public class RtpqGUI {

    private final RynixRtpqPlugin plugin;
    private final ConfigManager configManager;

    private ItemStack fillerCache;

    public RtpqGUI(RynixRtpqPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        cacheFiller();
    }

    private void cacheFiller() {
        try {
            String matName = configManager.isMenuFillerEnable() ? configManager.getMenuFillerMaterial() : "BLACK_STAINED_GLASS_PANE";
            Material mat = Material.valueOf(matName);
            fillerCache = new ItemStack(mat);
            ItemMeta meta = fillerCache.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                fillerCache.setItemMeta(meta);
            }
        } catch (Exception e) {
            fillerCache = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
            ItemMeta meta = fillerCache.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                fillerCache.setItemMeta(meta);
            }
        }
    }

    public void openGUI(Player player) {
        Inventory inv = createInventory(player);
        plugin.getScheduler().runForEntity(plugin, player, () -> player.openInventory(inv));
    }

    private Inventory createInventory(Player viewer) {
        String title = ChatUtil.color(configManager.getMenuTitle());
        int size = configManager.getMenuSize();
        // Ensure size is multiple of 9
        if (size % 9 != 0) size = 27;
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Fill border if enabled
        if (configManager.isMenuFillerEnable() && fillerCache != null) {
            for (int i = 0; i < size; i++) {
                // Fill only empty slots later, but for now fill all then overwrite items
                if (configManager.getMenuItems().values().stream().noneMatch(item -> item.slot == i)) {
                    // Check if slot is not used by menu items
                    inv.setItem(i, fillerCache);
                }
            }
        }

        // Build items from config
        for (ConfigManager.MenuItem menuItem : configManager.getMenuItems().values()) {
            ItemStack item = buildMenuItem(menuItem, viewer);
            if (menuItem.slot >= 0 && menuItem.slot < size) {
                inv.setItem(menuItem.slot, item);
            }
        }

        // Also add player heads preview if queue not too large and menu size is 27+? We'll replace some filler if needed
        // For duo mode, show queued players in extra slots if available (if menu size 27, we have slots 0-26, but config uses 10,12,13,14,16,4)
        // We'll not override existing menu items with player heads, but we can use empty slots to show queued players as extra info
        // For now keep simple as per McPlugin format

        return inv;
    }

    private ItemStack buildMenuItem(ConfigManager.MenuItem menuItem, Player viewer) {
        Material mat;
        try {
            mat = Material.valueOf(menuItem.material.toUpperCase());
        } catch (Exception e) {
            mat = Material.STONE;
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Placeholders
        String display = menuItem.displayName;
        List<String> lore = new ArrayList<>(menuItem.lore);

        // Common placeholders
        int total = plugin.getQueueManager().getTotalQueueSize();
        String world = configManager.getWorldDefault();
        QueuedPlayer qp = plugin.getQueueManager().getQueuedPlayer(viewer.getUniqueId());
        if (qp != null) world = qp.getWorldName();

        ConfigManager.WorldConfig wc = configManager.getWorldConfig(world);
        int require = wc != null ? wc.require : configManager.getDuoSize();
        long worldCount = plugin.getQueueManager().getQueueSnapshot().stream().filter(q -> q.getWorldName().equalsIgnoreCase(world)).count();
        int need = Math.max(0, require - (int) worldCount);
        int pos = plugin.getQueueManager().getPosition(viewer.getUniqueId());
        if (pos == -1) pos = total + 1;

        // Replace in display and lore
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("world", world);
        placeholders.put("count", String.valueOf(total));
        placeholders.put("need", String.valueOf(need));
        placeholders.put("require", String.valueOf(require));
        placeholders.put("position", String.valueOf(pos));
        placeholders.put("interval", String.valueOf(wc != null ? wc.interval : configManager.getQueueDelay()));
        placeholders.put("radius", wc != null ? String.valueOf(wc.maxRadius) : "500");
        placeholders.put("x", wc != null ? String.valueOf(wc.centerX) : "0");
        placeholders.put("z", wc != null ? String.valueOf(wc.centerZ) : "0");
        placeholders.put("spread", String.valueOf(configManager.getDuoSpreadDistance()));

        display = replacePlaceholders(display, placeholders);
        meta.setDisplayName(ChatUtil.color(display));

        List<String> newLore = new ArrayList<>();
        for (String line : lore) {
            newLore.add(ChatUtil.color(replacePlaceholders(line, placeholders)));
        }
        meta.setLore(newLore);
        item.setItemMeta(meta);
        return item;
    }

    private String replacePlaceholders(String text, Map<String, String> map) {
        if (text == null) return "";
        for (var e : map.entrySet()) {
            text = text.replace("{" + e.getKey() + "}", e.getValue());
            text = text.replace("${" + e.getKey() + "}", e.getValue());
        }
        return text;
    }

    // Legacy helpers for old GUI - still usable
    private ItemStack createInfoItem(Player viewer) { return new ItemStack(Material.CLOCK); }
    private ItemStack createJoinItem(Player viewer) { return new ItemStack(Material.ENDER_EYE); }
    private ItemStack createLeaveItem(Player viewer) { return new ItemStack(Material.BARRIER); }
    private ItemStack createWorldItem(ConfigManager.WorldConfig wc) { return new ItemStack(Material.GRASS_BLOCK); }
    private ItemStack createPlayerHeadItem(QueuedPlayer qp, int position) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            try { meta.setOwningPlayer(Bukkit.getOfflinePlayer(qp.getUuid())); } catch (Exception ignored) {}
            meta.setDisplayName(ChatUtil.color("&e" + qp.getName()));
            head.setItemMeta(meta);
        }
        return head;
    }
    private ItemStack createRefreshItem() { return new ItemStack(Material.SUNFLOWER); }
    private ItemStack createCloseItem() { return new ItemStack(Material.REDSTONE); }
}
