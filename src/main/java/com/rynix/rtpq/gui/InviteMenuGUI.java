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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

/**
 * InviteMenu GUI - Paginated player list for duo invite
 * McPlugin format: next slot 53, previous slot 45
 * Plugin: RynixRtpq | Author: RinZz
 */
public class InviteMenuGUI {

    private final RynixRtpqPlugin plugin;
    private final ConfigManager configManager;
    private final Map<UUID, Integer> playerPages = new HashMap<>();

    public InviteMenuGUI(RynixRtpqPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void openGUI(Player player, int page) {
        String title = ChatUtil.color(configManager.getInviteMenuTitle());
        int size = configManager.getInviteMenuSize();
        if (size % 9 != 0) size = 54;
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Filler
        if (configManager.isInviteMenuFillerEnable()) {
            try {
                Material fillerMat = Material.valueOf(configManager.getInviteMenuFillerMaterial());
                ItemStack filler = new ItemStack(fillerMat);
                ItemMeta meta = filler.getItemMeta();
                if (meta != null) { meta.setDisplayName(" "); filler.setItemMeta(meta); }
                for (int i = 0; i < size; i++) inv.setItem(i, filler);
            } catch (Exception ignored) {}
        }

        // Get online players excluding self and those already in queue? Show all online
        List<Player> online = new ArrayList<>(Bukkit.getOnlinePlayers());
        online.removeIf(p -> p.getUniqueId().equals(player.getUniqueId()));

        int perPage = size - 9; // Reserve last row for navigation
        int maxPage = (int) Math.ceil((double) online.size() / perPage);
        if (maxPage == 0) maxPage = 1;
        if (page < 0) page = 0;
        if (page >= maxPage) page = maxPage - 1;

        playerPages.put(player.getUniqueId(), page);

        int start = page * perPage;
        int end = Math.min(start + perPage, online.size());

        for (int i = start; i < end; i++) {
            Player target = online.get(i);
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                try { meta.setOwningPlayer(target); } catch (Exception ignored) {}
                meta.setDisplayName(ChatUtil.color("&e" + target.getName()));
                meta.setLore(List.of(
                        ChatUtil.color("&7Click để mời duo"),
                        ChatUtil.color("&7Cùng tele 1 chỗ với bạn"),
                        ChatUtil.color("&8" + target.getUniqueId())
                ));
                head.setItemMeta(meta);
            }
            inv.setItem(i - start, head);
        }

        // Navigation items
        ConfigManager.MenuItem nextItem = configManager.getInviteNextItem();
        ConfigManager.MenuItem prevItem = configManager.getInvitePrevItem();
        ConfigManager.MenuItem closeItem = configManager.getInviteCloseItem();

        if (nextItem != null && page < maxPage - 1) {
            inv.setItem(nextItem.slot, buildItem(nextItem));
        }
        if (prevItem != null && page > 0) {
            inv.setItem(prevItem.slot, buildItem(prevItem));
        }
        if (closeItem != null) {
            inv.setItem(closeItem.slot, buildItem(closeItem));
        } else {
            // Default close at 49
            ItemStack close = new ItemStack(Material.BARRIER);
            ItemMeta meta = close.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatUtil.color("&cĐóng"));
                close.setItemMeta(meta);
            }
            inv.setItem(49, close);
        }

        plugin.getScheduler().runForEntity(plugin, player, () -> player.openInventory(inv));
    }

    public void openGUI(Player player) {
        openGUI(player, playerPages.getOrDefault(player.getUniqueId(), 0));
    }

    public int getPage(Player player) {
        return playerPages.getOrDefault(player.getUniqueId(), 0);
    }

    private ItemStack buildItem(ConfigManager.MenuItem menuItem) {
        Material mat;
        try { mat = Material.valueOf(menuItem.material.toUpperCase()); } catch (Exception e) { mat = Material.STONE; }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(ChatUtil.color(menuItem.displayName));
        List<String> lore = new ArrayList<>();
        for (String line : menuItem.lore) lore.add(ChatUtil.color(line));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
