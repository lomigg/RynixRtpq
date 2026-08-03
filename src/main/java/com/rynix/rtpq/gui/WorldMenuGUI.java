package com.rynix.rtpq.gui;

import com.rynix.rtpq.RynixRtpqPlugin;
import com.rynix.rtpq.config.ConfigManager;
import com.rynix.rtpq.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * WorldMenu GUI - McPlugin format
 * Plugin: RynixRtpq | Author: RinZz
 */
public class WorldMenuGUI {

    private final RynixRtpqPlugin plugin;
    private final ConfigManager configManager;

    public WorldMenuGUI(RynixRtpqPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void openGUI(Player player) {
        String title = ChatUtil.color(configManager.getWorldMenuTitle());
        int size = configManager.getWorldMenuSize();
        if (size % 9 != 0) size = 9;
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Filler
        if (configManager.isWorldMenuFillerEnable()) {
            try {
                Material fillerMat = Material.valueOf(configManager.getWorldMenuFillerMaterial());
                ItemStack filler = new ItemStack(fillerMat);
                ItemMeta meta = filler.getItemMeta();
                if (meta != null) { meta.setDisplayName(" "); filler.setItemMeta(meta); }
                for (int i = 0; i < size; i++) inv.setItem(i, filler);
            } catch (Exception ignored) {}
        }

        // Add worlds
        int slot = 0;
        for (ConfigManager.WorldConfig wc : configManager.getWorldConfigs().values()) {
            if (slot >= size) break;
            ItemStack item = createWorldItem(wc);
            inv.setItem(slot, item);
            slot++;
        }

        plugin.getScheduler().runForEntity(plugin, player, () -> player.openInventory(inv));
    }

    private ItemStack createWorldItem(ConfigManager.WorldConfig wc) {
        // Use material from config world-menu.world.material else grass
        Material mat = Material.GRASS_BLOCK;
        String display = "&a" + wc.name;
        List<String> lore = List.of("&fClick to select this world");

        // Try to get from config world-menu.world template
        // For simplicity use GRASS_BLOCK and placeholders
        try {
            // Could read from config but use wc values
            mat = Material.GRASS_BLOCK;
            if (wc.name.toLowerCase().contains("nether")) mat = Material.NETHERRACK;
            else if (wc.name.toLowerCase().contains("end")) mat = Material.END_STONE;
        } catch (Exception ignored) {}

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String displayTemplate = plugin.getConfig().getString("world-menu.world.display_name", "&a{world}");
        display = displayTemplate.replace("{world}", wc.name);

        List<String> loreTemplate = plugin.getConfig().getStringList("world-menu.world.lore");
        if (loreTemplate.isEmpty()) loreTemplate = Arrays.asList("&7Center: {x}, {z}", "&7Radius: {radius}", "&7Require: {require}");

        Map<String, String> ph = new HashMap<>();
        ph.put("world", wc.name);
        ph.put("x", String.valueOf(wc.centerX));
        ph.put("z", String.valueOf(wc.centerZ));
        ph.put("radius", String.valueOf(wc.maxRadius));
        ph.put("require", String.valueOf(wc.require));
        ph.put("interval", String.valueOf(wc.interval));

        meta.setDisplayName(ChatUtil.color(replace(ph, display)));
        List<String> newLore = new ArrayList<>();
        for (String line : loreTemplate) newLore.add(ChatUtil.color(replace(ph, line)));
        meta.setLore(newLore);
        item.setItemMeta(meta);
        return item;
    }

    private String replace(Map<String, String> map, String text) {
        for (var e : map.entrySet()) text = text.replace("{" + e.getKey() + "}", e.getValue());
        return text;
    }
}
