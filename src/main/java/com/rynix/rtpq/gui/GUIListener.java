package com.rynix.rtpq.gui;

import com.rynix.rtpq.RynixRtpqPlugin;
import com.rynix.rtpq.config.ConfigManager;
import com.rynix.rtpq.utils.ChatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * GUI Listener v2.0 - Handles Main Menu, World Menu, Invite Menu
 * McPlugin format + RynixRtpq Duo
 * Author: RinZz
 */
public class GUIListener implements Listener {

    private final RynixRtpqPlugin plugin;
    private final RtpqGUI mainGui;
    private final WorldMenuGUI worldMenuGUI;
    private final InviteMenuGUI inviteMenuGUI;

    public GUIListener(RynixRtpqPlugin plugin, RtpqGUI mainGui, WorldMenuGUI worldMenuGUI, InviteMenuGUI inviteMenuGUI) {
        this.plugin = plugin;
        this.mainGui = mainGui;
        this.worldMenuGUI = worldMenuGUI;
        this.inviteMenuGUI = inviteMenuGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        String title = e.getView().getTitle();
        String mainTitle = ChatUtil.color(plugin.getConfigManager().getMenuTitle());
        String worldTitle = ChatUtil.color(plugin.getConfigManager().getWorldMenuTitle());
        String inviteTitle = ChatUtil.color(plugin.getConfigManager().getInviteMenuTitle());

        boolean isMain = title.equals(mainTitle);
        boolean isWorld = title.equals(worldTitle);
        boolean isInvite = title.equals(inviteTitle);

        if (!isMain && !isWorld && !isInvite) return;

        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        plugin.getSoundManager().playSound(player, "click");

        int slot = e.getSlot();

        if (isMain) {
            handleMainMenuClick(player, slot, clicked);
        } else if (isWorld) {
            handleWorldMenuClick(player, clicked);
        } else if (isInvite) {
            handleInviteMenuClick(player, slot, clicked);
        }
    }

    private void handleMainMenuClick(Player player, int slot, ItemStack clicked) {
        ConfigManager configManager = plugin.getConfigManager();

        // Find which menu item slot matches
        for (ConfigManager.MenuItem item : configManager.getMenuItems().values()) {
            if (item.slot == slot) {
                String key = item.key.toLowerCase();
                switch (key) {
                    case "leave":
                        plugin.getQueueManager().removeFromQueue(player.getUniqueId());
                        plugin.getScheduler().runDelayedForEntity(plugin, player, () -> mainGui.openGUI(player), 5L, () -> {});
                        break;
                    case "join":
                        if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
                            ChatUtil.send(player, configManager.getMessage("already"));
                        } else {
                            if (configManager.isCooldownEnabled() && plugin.getCooldownManager().hasCooldown(player.getUniqueId()) && !player.hasPermission("rynixrtpq.bypass.cooldown") && !player.hasPermission("rtpqueue.bypass.cooldown") && !player.hasPermission("rtpq.bypass.cooldown")) {
                                long rem = plugin.getCooldownManager().getRemaining(player.getUniqueId());
                                ChatUtil.send(player, configManager.getMessage("cooldown", java.util.Map.of("time", String.valueOf(rem))));
                            } else {
                                plugin.getQueueManager().addToQueue(player, configManager.getWorldDefault());
                            }
                        }
                        plugin.getScheduler().runDelayedForEntity(plugin, player, () -> mainGui.openGUI(player), 5L, () -> {});
                        break;
                    case "world":
                        worldMenuGUI.openGUI(player);
                        break;
                    case "queue":
                        // Refresh or show queue list via /rtpqueue list?
                        mainGui.openGUI(player);
                        break;
                    case "invite":
                        if (!plugin.getQueueManager().isInQueue(player.getUniqueId())) {
                            ChatUtil.send(player, configManager.getMessage("include"));
                            return;
                        }
                        inviteMenuGUI.openGUI(player, 0);
                        break;
                    case "duo-info":
                        // Just refresh
                        mainGui.openGUI(player);
                        break;
                    default:
                        // Unknown, try to close if barrier?
                        if (clicked.getType() == Material.BARRIER) {
                            player.closeInventory();
                        }
                        break;
                }
                return;
            }
        }

        // Filler click does nothing, but if close item (custom)
        if (clicked.getType() == Material.BARRIER || clicked.getType() == Material.RED_STAINED_GLASS_PANE && ChatUtil.color(clicked.getItemMeta() != null ? clicked.getItemMeta().getDisplayName() : "").toLowerCase().contains("close") || clicked.getType().name().contains("RED")) {
            // Check if it's leave or close - leave already handled, so if slot not in menuItems, close
            if (configManager.getMenuItems().values().stream().noneMatch(mi -> mi.slot == slot)) {
                // Maybe close?
            }
        }
    }

    private void handleWorldMenuClick(Player player, ItemStack clicked) {
        if (clicked.getItemMeta() == null) return;
        String display = ChatUtil.color(clicked.getItemMeta().getDisplayName());

        // Try to extract world name from display or from config
        for (ConfigManager.WorldConfig wc : plugin.getConfigManager().getWorldConfigs().values()) {
            if (display.toLowerCase().contains(wc.name.toLowerCase())) {
                // Join queue with this world
                if (plugin.getQueueManager().isInQueue(player.getUniqueId())) {
                    ChatUtil.send(player, plugin.getConfigManager().getMessage("already"));
                } else {
                    plugin.getQueueManager().addToQueue(player, wc.name);
                }
                player.closeInventory();
                // Open main GUI after
                plugin.getScheduler().runDelayedForEntity(plugin, player, () -> mainGui.openGUI(player), 10L, () -> {});
                return;
            }
        }

        // If clicked filler, do nothing
    }

    private void handleInviteMenuClick(Player player, int slot, ItemStack clicked) {
        ConfigManager configManager = plugin.getConfigManager();

        // Check navigation
        ConfigManager.MenuItem nextItem = configManager.getInviteNextItem();
        ConfigManager.MenuItem prevItem = configManager.getInvitePrevItem();
        ConfigManager.MenuItem closeItem = configManager.getInviteCloseItem();

        if (nextItem != null && slot == nextItem.slot) {
            int currentPage = inviteMenuGUI.getPage(player);
            inviteMenuGUI.openGUI(player, currentPage + 1);
            return;
        }
        if (prevItem != null && slot == prevItem.slot) {
            int currentPage = inviteMenuGUI.getPage(player);
            inviteMenuGUI.openGUI(player, currentPage - 1);
            return;
        }
        if (closeItem != null && slot == closeItem.slot) {
            player.closeInventory();
            return;
        }
        if (slot == 49 && closeItem == null) {
            player.closeInventory();
            return;
        }

        // If player head clicked, invite that player
        if (clicked.getType() == Material.PLAYER_HEAD) {
            if (clicked.getItemMeta() != null) {
                String display = clicked.getItemMeta().getDisplayName();
                // Extract player name from display (strip colors)
                String targetName = org.bukkit.ChatColor.stripColor(display);
                if (targetName == null || targetName.isEmpty()) return;

                Player target = Bukkit.getPlayer(targetName);
                if (target == null) {
                    // Try to get from lore UUID?
                    if (clicked.getItemMeta().getLore() != null) {
                        for (String loreLine : clicked.getItemMeta().getLore()) {
                            String stripped = org.bukkit.ChatColor.stripColor(loreLine);
                            if (stripped != null && stripped.length() > 20) { // UUID length
                                try {
                                    java.util.UUID uuid = java.util.UUID.fromString(stripped);
                                    target = Bukkit.getPlayer(uuid);
                                    if (target != null) break;
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                }

                if (target == null) {
                    ChatUtil.send(player, plugin.getConfigManager().getMessage("non-player"));
                    return;
                }

                plugin.getInviteManager().sendInvite(player, target);
                player.closeInventory();
            }
        }
    }
}
