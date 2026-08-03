package com.rynix.rtpq.queue;

import com.rynix.rtpq.RynixRtpqPlugin;
import com.rynix.rtpq.config.ConfigManager;
import com.rynix.rtpq.utils.ChatUtil;
import com.rynix.rtpq.utils.LoggerUtil;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QueueManager v2.0 - McPlugin format + RynixRtpq Duo
 * Supports per-world require (2 players same location), interval, unsafe-block
 * Author: RinZz - Rynix Studio
 */
public class QueueManager {

    private final RynixRtpqPlugin plugin;
    private final ConfigManager configManager;

    // Per world queue? We'll have global queue but grouped by world target for matching
    // For simplicity, keep global priority + normal but matching per world
    private final Queue<QueuedPlayer> normalQueue = new ConcurrentLinkedQueue<>();
    private final Queue<QueuedPlayer> priorityQueue = new ConcurrentLinkedQueue<>();
    private final Map<UUID, QueuedPlayer> queueLookup = new ConcurrentHashMap<>();
    private final Map<UUID, Long> invulnerablePlayers = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    // Teleport countdown per world match
    private final Map<String, Integer> teleportCountdowns = new ConcurrentHashMap<>();

    private boolean processing = false;
    private long lastTeleportTime = 0;

    public QueueManager(RynixRtpqPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void startQueueTask() {
        long delay = configManager.getQueueDelay() * 20L;
        plugin.getScheduler().runTimer(plugin, this::processQueue, delay, delay);
        plugin.getScheduler().runTimer(plugin, this::updatePlayersDisplay, 0L, 20L);
        plugin.getScheduler().runTimer(plugin, this::cleanupInvulnerability, 20L, 20L);
        // ActionBar countdown for teleport interval
        plugin.getScheduler().runTimer(plugin, this::processCountdowns, 0L, 20L);
    }

    private void processCountdowns() {
        // If we have countdowns for found matches, handle actionbar
        for (Iterator<Map.Entry<String, Integer>> it = teleportCountdowns.entrySet().iterator(); it.hasNext();) {
            Map.Entry<String, Integer> entry = it.next();
            String key = entry.getKey(); // world name or batch id
            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
                continue;
            }
            entry.setValue(remaining);
            // Send action bar to those in countdown? For now broadcast to queued players of that world
            String msgTemplate = configManager.getActionBarMessage();
            String formatted = msgTemplate.replace("${interval}", String.valueOf(remaining))
                    .replace("{count}", String.valueOf(getTotalQueueSize()))
                    .replace("{need}", "0");

            for (QueuedPlayer qp : queueLookup.values()) {
                if (qp.getWorldName().equalsIgnoreCase(key) || key.startsWith("batch_")) {
                    Player p = Bukkit.getPlayer(qp.getUuid());
                    if (p != null) {
                        ChatUtil.sendActionBar(p, formatted);
                        plugin.getSoundManager().playSound(p, "time");
                    }
                }
            }
        }
    }

    private void processQueue() {
        if (processing) return;

        long now = System.currentTimeMillis();
        long requiredDelay = configManager.getQueueDelay() * 1000L;
        if (now - lastTeleportTime < requiredDelay) return;

        // Group by world
        Map<String, List<QueuedPlayer>> byWorld = new HashMap<>();
        for (QueuedPlayer qp : getQueueSnapshot()) {
            byWorld.computeIfAbsent(qp.getWorldName().toLowerCase(), k -> new ArrayList<>()).add(qp);
        }

        // For each world, check if enough players for require
        for (Map.Entry<String, List<QueuedPlayer>> entry : byWorld.entrySet()) {
            String worldName = entry.getKey();
            ConfigManager.WorldConfig wc = configManager.getWorldConfig(worldName);
            if (wc == null) continue;
            int require = wc.require;
            List<QueuedPlayer> list = entry.getValue();

            if (list.size() >= require) {
                // Found a match - process duo/squad
                processWorldBatch(wc, list.subList(0, require));
                return; // Process one batch per tick to respect delay
            }
        }

        // If no world batch ready, check if duo solo timeout should trigger (if enabled and solo-after >0)
        // For McPlugin strict mode, we don't solo, but if solo-after enabled we could
        // Here we respect duoSoloAfterSeconds if config says solo-after >0
        if (configManager.getDuoSoloAfterSeconds() > 0 && getTotalQueueSize() > 0) {
            QueuedPlayer oldest = getOldestPlayer();
            if (oldest != null && oldest.getWaitTimeSeconds() >= configManager.getDuoSoloAfterSeconds()) {
                processSoloQueue();
            }
        }
    }

    private void processWorldBatch(ConfigManager.WorldConfig wc, List<QueuedPlayer> batch) {
        if (batch.isEmpty()) return;

        // Filter offline
        List<QueuedPlayer> validBatch = new ArrayList<>();
        for (QueuedPlayer qp : batch) {
            Player p = Bukkit.getPlayer(qp.getUuid());
            if (p != null && p.isOnline()) validBatch.add(qp);
            else queueLookup.remove(qp.getUuid());
        }

        if (validBatch.size() < wc.require) {
            // Not enough valid, put back? Actually they were already removed from queue snapshot but not from queue itself
            // We need to ensure we pull from actual queues
            // Let's get fresh batch from queues
            return;
        }

        processing = true;
        lastTeleportTime = System.currentTimeMillis();

        List<Player> onlinePlayers = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (QueuedPlayer qp : validBatch) {
            Player p = Bukkit.getPlayer(qp.getUuid());
            if (p != null) {
                onlinePlayers.add(p);
                names.add(p.getName());
            }
        }

        if (onlinePlayers.isEmpty()) {
            processing = false;
            return;
        }

        String playerList = String.join(", ", names);
        LoggerUtil.info("Found match in world " + wc.name + ": " + playerList + " | Require: " + wc.require + " | RynixRtpq Duo by RinZz");

        // Broadcast found
        String broadcastFoundTitle = configManager.getBroadcastFoundTitle().replace("{world}", wc.name);
        String broadcastFoundMsg = configManager.getBroadcastFoundMessage().replace("{player_list}", playerList).replace("{world}", wc.name);

        for (Player p : Bukkit.getOnlinePlayers()) {
            ChatUtil.sendRaw(p, broadcastFoundMsg);
            if (configManager.isTitleEnable() && !broadcastFoundTitle.isEmpty()) {
                try {
                    p.sendTitle(ChatUtil.color(broadcastFoundTitle), ChatUtil.color(configManager.getBroadcastFoundMessage().replace("{player_list}", playerList).replace("{world}", wc.name)), 10, 60, 10);
                } catch (Exception ignored) {}
            }
        }

        // Start countdown interval from world config
        int interval = wc.interval;
        if (interval > 0) {
            teleportCountdowns.put(wc.name.toLowerCase(), interval);
            // Schedule actual teleport after interval
            plugin.getScheduler().runDelayed(plugin, () -> {
                executeDuoTeleport(wc, validBatch, onlinePlayers);
            }, interval * 20L);
        } else {
            executeDuoTeleport(wc, validBatch, onlinePlayers);
        }
    }

    private void executeDuoTeleport(ConfigManager.WorldConfig wc, List<QueuedPlayer> validBatch, List<Player> onlinePlayers) {
        plugin.getScheduler().runAsync(plugin, () -> {
            plugin.getRTPService().teleportDuoWithRetry(onlinePlayers, wc.name, () -> {
                for (QueuedPlayer qp : validBatch) {
                    removeFromQueueInternal(qp.getUuid());
                }
                // Broadcast success title
                for (Player p : onlinePlayers) {
                    if (configManager.isTitleEnable()) {
                        try {
                            String title = configManager.getTitleTeleported().isEmpty() ? "&a&lDUO!" : configManager.getTitleTeleported();
                            String subtitle = configManager.getSubtitleTeleported().replace("{player_list}", String.join(", ", onlinePlayers.stream().map(Player::getName).toList())).replace("{world}", wc.name);
                            p.sendTitle(ChatUtil.color(title), ChatUtil.color(subtitle), 10, 60, 20);
                        } catch (Exception ignored) {}
                    }
                    plugin.getSoundManager().playSound(p, "success", "teleport");
                }
                processing = false;
            }, () -> {
                for (QueuedPlayer qp : validBatch) {
                    Player p = Bukkit.getPlayer(qp.getUuid());
                    if (p != null) {
                        ChatUtil.send(p, configManager.getMessage("location", Map.of("attempts", String.valueOf(wc.maxAttempts))));
                    }
                    removeFromQueueInternal(qp.getUuid());
                }
                processing = false;
            });
        });
    }

    private void processSoloQueue() {
        QueuedPlayer next = getNextPlayer();
        if (next == null) return;

        Player player = Bukkit.getPlayer(next.getUuid());
        if (player == null || !player.isOnline()) {
            removeFromQueueInternal(next.getUuid());
            return;
        }

        processing = true;
        lastTeleportTime = System.currentTimeMillis();

        plugin.getScheduler().runAsync(plugin, () -> {
            plugin.getRTPService().teleportWithRetry(player, next.getWorldName());
            removeFromQueueInternal(next.getUuid());
            processing = false;
        });
    }

    public QueuedPlayer getNextPlayer() {
        QueuedPlayer p = priorityQueue.poll();
        if (p != null) return p;
        return normalQueue.poll();
    }

    public List<QueuedPlayer> getNextPlayers(int count) {
        List<QueuedPlayer> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            QueuedPlayer qp = getNextPlayer();
            if (qp == null) break;
            list.add(qp);
        }
        return list;
    }

    public QueuedPlayer getOldestPlayer() {
        return queueLookup.values().stream()
                .min(Comparator.comparingLong(QueuedPlayer::getJoinTime))
                .orElse(null);
    }

    public boolean addToQueue(Player player, String worldName) {
        UUID uuid = player.getUniqueId();

        if (queueLookup.containsKey(uuid)) {
            ChatUtil.send(player, configManager.getMessage("already"));
            return false;
        }

        if (getTotalQueueSize() >= configManager.getMaxQueueSize()) {
            ChatUtil.send(player, configManager.getMessage("queue-full", Map.of("max", String.valueOf(configManager.getMaxQueueSize()))));
            return false;
        }

        ConfigManager.WorldConfig wc = configManager.getWorldConfig(worldName);
        if (wc == null) {
            wc = configManager.getWorldConfig(configManager.getWorldDefault());
            if (wc == null) {
                if (!configManager.getWorldConfigs().isEmpty()) {
                    wc = configManager.getWorldConfigs().values().iterator().next();
                } else {
                    ChatUtil.send(player, configManager.getMessage("world-error", Map.of("world", worldName)));
                    return false;
                }
            }
            worldName = wc.name;
        }

        boolean isPriority = player.hasPermission("rynixrtpq.priority") || player.hasPermission("rtpqueue.priority");

        QueuedPlayer qp = new QueuedPlayer(uuid, player.getName(), worldName, isPriority);

        if (isPriority) priorityQueue.offer(qp);
        else normalQueue.offer(qp);

        queueLookup.put(uuid, qp);

        int position = getPosition(uuid);
        int total = getTotalQueueSize();
        int require = wc.require;
        int need = Math.max(0, require - total); // Actually need for whole queue, but per world need logic

        // Count per world
        long worldCount = getQueueSnapshot().stream().filter(q -> q.getWorldName().equalsIgnoreCase(worldName)).count();
        int worldNeed = Math.max(0, require - (int) worldCount);

        if (worldCount >= require) {
            ChatUtil.send(player, configManager.getMessage("join", Map.of("position", String.valueOf(position), "total", String.valueOf(total), "need", String.valueOf(worldNeed), "world", worldName)));
        } else {
            String joinDuoMsg = configManager.getMessage("join-duo", Map.of("position", String.valueOf(position), "total", String.valueOf(total), "need", String.valueOf(worldNeed), "world", worldName));
            if (joinDuoMsg.contains("not found")) {
                ChatUtil.send(player, configManager.getMessage("join", Map.of("position", String.valueOf(position), "total", String.valueOf(total), "need", String.valueOf(worldNeed), "world", worldName)));
            } else {
                ChatUtil.send(player, joinDuoMsg);
            }
        }

        // Broadcast join
        String broadcastJoin = configManager.getBroadcastJoin()
                .replace("{player}", player.getName())
                .replace("{world}", worldName)
                .replace("{count}", String.valueOf(worldCount))
                .replace("{require}", String.valueOf(require));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(uuid)) {
                ChatUtil.sendRaw(p, broadcastJoin);
            }
        }

        if (configManager.isBossBarEnabled()) {
            createBossBar(player, wc);
        }

        plugin.getSoundManager().playSound(player, "join");

        LoggerUtil.info(player.getName() + " joined queue for world " + worldName + " (Pos: " + position + " | WorldCount: " + worldCount + "/" + require + ") | RynixRtpq Duo | Author: RinZz");
        return true;
    }

    public boolean removeFromQueue(UUID uuid) {
        boolean result = removeFromQueueInternal(uuid);
        if (result) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                ChatUtil.send(player, configManager.getMessage("leave"));
                plugin.getSoundManager().playSound(player, "leave");
            }
        }
        return result;
    }

    private boolean removeFromQueueInternal(UUID uuid) {
        QueuedPlayer qp = queueLookup.remove(uuid);
        if (qp == null) return false;
        normalQueue.remove(qp);
        priorityQueue.remove(qp);
        BossBar bar = bossBars.remove(uuid);
        if (bar != null) bar.removeAll();
        plugin.getInviteManager().removeInvites(uuid);
        return true;
    }

    public boolean isInQueue(UUID uuid) { return queueLookup.containsKey(uuid); }
    public QueuedPlayer getQueuedPlayer(UUID uuid) { return queueLookup.get(uuid); }

    public int getPosition(UUID uuid) {
        if (!queueLookup.containsKey(uuid)) return -1;
        int pos = 0;
        for (QueuedPlayer qp : priorityQueue) {
            pos++;
            if (qp.getUuid().equals(uuid)) return pos;
        }
        for (QueuedPlayer qp : normalQueue) {
            pos++;
            if (qp.getUuid().equals(uuid)) return pos;
        }
        return -1;
    }

    public int getTotalQueueSize() { return priorityQueue.size() + normalQueue.size(); }

    public List<QueuedPlayer> getQueueSnapshot() {
        List<QueuedPlayer> list = new ArrayList<>(getTotalQueueSize());
        list.addAll(priorityQueue);
        list.addAll(normalQueue);
        return Collections.unmodifiableList(list);
    }

    public void clearQueue() {
        int count = getTotalQueueSize();
        normalQueue.clear();
        priorityQueue.clear();
        queueLookup.clear();
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();
        LoggerUtil.info("Queue cleared: " + count + " players removed | RynixRtpq Duo by RinZz");
    }

    public void handleTeleportFail(Player player) {}

    private void updatePlayersDisplay() {
        if (getTotalQueueSize() == 0) return;
        if (!configManager.isBossBarEnabled() && !configManager.isActionBarEnabled()) return;

        int total = getTotalQueueSize();

        for (QueuedPlayer qp : queueLookup.values()) {
            Player p = Bukkit.getPlayer(qp.getUuid());
            if (p == null || !p.isOnline()) continue;

            int pos = getPosition(qp.getUuid());
            if (pos == -1) continue;

            ConfigManager.WorldConfig wc = configManager.getWorldConfig(qp.getWorldName());
            int require = wc != null ? wc.require : configManager.getDuoSize();
            long worldCount = getQueueSnapshot().stream().filter(q -> q.getWorldName().equalsIgnoreCase(qp.getWorldName())).count();
            int need = Math.max(0, require - (int) worldCount);

            if (configManager.isBossBarEnabled()) {
                BossBar bar = bossBars.get(qp.getUuid());
                if (bar != null) {
                    String title = configManager.getBossBarTitle()
                            .replace("{count}", String.valueOf(worldCount))
                            .replace("{need}", String.valueOf(need))
                            .replace("{world}", qp.getWorldName())
                            .replace("{position}", String.valueOf(pos))
                            .replace("{total}", String.valueOf(total));
                    bar.setTitle(ChatUtil.color(title));
                    double progress = Math.max(0.1, (double) worldCount / require);
                    bar.setProgress(progress);
                }
            }
        }
    }

    private void createBossBar(Player player, ConfigManager.WorldConfig wc) {
        if (!configManager.isBossBarEnable()) return;
        int require = wc.require;
        long worldCount = getQueueSnapshot().stream().filter(q -> q.getWorldName().equalsIgnoreCase(wc.name)).count();
        int need = Math.max(0, require - (int) worldCount);

        String title = configManager.getBossBarTitle()
                .replace("{count}", String.valueOf(worldCount))
                .replace("{need}", String.valueOf(need))
                .replace("{world}", wc.name);

        BarColor color;
        try {
            color = BarColor.valueOf(configManager.getBossBarColor().toUpperCase());
        } catch (Exception e) {
            color = BarColor.RED;
        }
        BarStyle style;
        try {
            style = BarStyle.valueOf(configManager.getBossBarStyle().toUpperCase());
        } catch (Exception e) {
            style = BarStyle.SOLID;
        }

        BossBar bar = Bukkit.createBossBar(ChatUtil.color(title), color, style);
        bar.addPlayer(player);
        bossBars.put(player.getUniqueId(), bar);
    }

    public void giveInvulnerability(Player player) {
        if (!configManager.isInvulnerabilityEnabled()) return;
        long expire = System.currentTimeMillis() + (configManager.getInvulnerabilitySeconds() * 1000L);
        invulnerablePlayers.put(player.getUniqueId(), expire);
    }

    public boolean isInvulnerable(UUID uuid) {
        Long expire = invulnerablePlayers.get(uuid);
        if (expire == null) return false;
        if (System.currentTimeMillis() > expire) {
            invulnerablePlayers.remove(uuid);
            return false;
        }
        return true;
    }

    private void cleanupInvulnerability() {
        long now = System.currentTimeMillis();
        invulnerablePlayers.entrySet().removeIf(entry -> {
            if (entry.getValue() < now) {
                Player p = Bukkit.getPlayer(entry.getKey());
                if (p != null) {
                    ChatUtil.send(p, configManager.getMessage("invulnerability-end", Map.of()));
                }
                return true;
            }
            return false;
        });
    }

    public void handlePlayerQuit(UUID uuid) {
        removeFromQueueInternal(uuid);
        BossBar bar = bossBars.remove(uuid);
        if (bar != null) bar.removeAll();
        invulnerablePlayers.remove(uuid);
    }

    public void handleDamage(Player player) {
        if (!configManager.isDamagedCancel()) return;
        if (isInQueue(player.getUniqueId())) {
            removeFromQueue(player.getUniqueId());
            ChatUtil.send(player, configManager.getMessage("damaged-cancel", Map.of()));
            plugin.getSoundManager().playSound(player, "break");
        }
    }

    public Map<UUID, BossBar> getBossBars() { return bossBars; }
}
