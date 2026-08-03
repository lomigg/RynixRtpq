package com.rynix.rtpq.rtp;

import com.rynix.rtpq.RynixRtpqPlugin;
import com.rynix.rtpq.config.ConfigManager;
import com.rynix.rtpq.utils.ChatUtil;
import com.rynix.rtpq.utils.LoggerUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

/**
 * RTP Service v2.0 - Supports per-world unsafe-block, require, interval
 * Duo Mode - 2+ players same location
 * Plugin: RynixRtpq | Author: RinZz
 */
public class RTPService {

    private final RynixRtpqPlugin plugin;
    private final ConfigManager configManager;

    private static final Set<Material> DEFAULT_UNSAFE = Set.of(
            Material.LAVA, Material.MAGMA_BLOCK, Material.CACTUS, Material.FIRE, Material.SOUL_FIRE
    );

    public RTPService(RynixRtpqPlugin plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public CompletableFuture<Location> findSafeLocationAsync(String worldName, Player player) {
        return findSafeLocationAsync(worldName);
    }

    public CompletableFuture<Location> findSafeLocationAsync(String worldName) {
        CompletableFuture<Location> future = new CompletableFuture<>();

        ConfigManager.WorldConfig wc = configManager.getWorldConfig(worldName);
        if (wc == null) {
            wc = configManager.getWorldConfig(configManager.getWorldDefault());
            if (wc == null) {
                if (!configManager.getWorldConfigs().isEmpty()) {
                    wc = configManager.getWorldConfigs().values().iterator().next();
                } else {
                    future.completeExceptionally(new IllegalStateException("No world config found"));
                    return future;
                }
            }
        }

        World world = plugin.getServer().getWorld(wc.name);
        if (world == null) {
            future.completeExceptionally(new IllegalStateException("World not found: " + wc.name));
            return future;
        }

        ConfigManager.WorldConfig finalWc = wc;
        World finalWorld = world;

        plugin.getScheduler().runAsync(plugin, () -> {
            Location loc = findSafeLocationSync(finalWorld, finalWc);
            if (loc != null) {
                loadChunkAndComplete(finalWorld, loc, finalWc, future);
            } else {
                future.complete(null);
            }
        });

        return future;
    }

    private void loadChunkAndComplete(World world, Location loc, ConfigManager.WorldConfig wc, CompletableFuture<Location> future) {
        try {
            world.getChunkAtAsync(loc).thenAccept(chunk -> {
                Location safe = getHighestSafeY(world, loc.getBlockX(), loc.getBlockZ(), loc.getBlockY(), wc);
                if (safe != null) {
                    plugin.getScheduler().runAtLocation(plugin, safe, () -> future.complete(safe));
                } else {
                    future.complete(loc);
                }
            }).exceptionally(ex -> {
                future.complete(loc);
                return null;
            });
        } catch (NoSuchMethodError | UnsupportedOperationException e) {
            future.complete(loc);
        } catch (Exception ex) {
            future.complete(loc);
        }
    }

    private Location findSafeLocationSync(World world, ConfigManager.WorldConfig wc) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int maxAttempts = wc.maxAttempts;
        int minRadius = wc.minRadius;
        int maxRadius = wc.maxRadius;
        int centerX = wc.centerX;
        int centerZ = wc.centerZ;

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = minRadius + random.nextDouble() * (maxRadius - minRadius);
            int x = centerX + (int) (radius * Math.cos(angle));
            int z = centerZ + (int) (radius * Math.sin(angle));
            int y = random.nextInt(wc.minY, wc.maxY + 1);

            if (y < world.getMinHeight() || y > world.getMaxHeight() - 2) continue;

            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            LoggerUtil.debug("Attempt " + (attempt+1) + " for " + wc.name + " at " + x + "," + y + "," + z, configManager.isDebug());
            return loc;
        }
        return null;
    }

    private Location getHighestSafeY(World world, int x, int z, int startY, ConfigManager.WorldConfig wc) {
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 2;
        int worldMin = wc.minY;
        int worldMax = wc.maxY;

        for (int y = Math.max(worldMin, startY); y <= Math.min(worldMax, maxY); y++) {
            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            if (isSafeLocation(loc, wc)) return loc;
        }
        for (int y = Math.min(worldMax, startY - 1); y >= Math.max(worldMin, minY); y--) {
            Location loc = new Location(world, x + 0.5, y, z + 0.5);
            if (isSafeLocation(loc, wc)) return loc;
        }
        try {
            int highest = world.getHighestBlockYAt(x, z);
            if (highest >= worldMin && highest <= worldMax) {
                Location loc = new Location(world, x + 0.5, highest + 1, z + 0.5);
                if (isSafeLocation(loc, wc)) return loc;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public boolean isSafeLocation(Location loc, ConfigManager.WorldConfig wc) {
        if (loc == null || loc.getWorld() == null) return false;
        World world = loc.getWorld();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        if (y < world.getMinHeight() || y > world.getMaxHeight() - 2) return false;
        if (!wc.safe) return true; // If safe = false, don't check safety

        try {
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            Block ground = world.getBlockAt(x, y - 1, z);

            if (!feet.getType().isAir() && feet.getType() != Material.WATER) return false;
            if (!head.getType().isAir()) return false;
            if (!ground.getType().isSolid() || ground.isLiquid()) {
                if (ground.getType() == Material.AIR) return false;
                if (!ground.getType().isSolid()) return false;
            }

            // Check unsafe-block list from config
            Set<String> unsafe = wc.unsafeBlocks;
            if (unsafe != null && !unsafe.isEmpty()) {
                if (unsafe.contains(feet.getType().name()) || unsafe.contains(head.getType().name()) || unsafe.contains(ground.getType().name())) {
                    return false;
                }
                // Also check VOID special
                if (unsafe.contains("VOID") && y <= world.getMinHeight() + 2) return false;
            }

            if (DEFAULT_UNSAFE.contains(feet.getType()) || DEFAULT_UNSAFE.contains(ground.getType())) return false;

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Legacy overload
    public boolean isSafeLocation(Location loc) {
        ConfigManager.WorldConfig wc = configManager.getWorldConfig(loc.getWorld() != null ? loc.getWorld().getName() : configManager.getWorldDefault());
        if (wc == null) return false;
        return isSafeLocation(loc, wc);
    }

    public void teleportPlayer(Player player, Location location, Runnable onSuccess, Runnable onFail) {
        if (player == null || location == null) {
            if (onFail != null) onFail.run();
            return;
        }

        plugin.getScheduler().runForEntity(plugin, player, () -> {
            try {
                try {
                    player.teleportAsync(location).thenAccept(result -> {
                        if (result) {
                            plugin.getScheduler().runForEntity(plugin, player, onSuccess);
                        } else {
                            plugin.getScheduler().runForEntity(plugin, player, onFail);
                        }
                    });
                } catch (NoSuchMethodError e) {
                    boolean success = player.teleport(location);
                    if (success) {
                        if (onSuccess != null) onSuccess.run();
                    } else {
                        if (onFail != null) onFail.run();
                    }
                }
            } catch (Exception ex) {
                LoggerUtil.warn("Teleport failed for " + player.getName() + ": " + ex.getMessage());
                if (onFail != null) onFail.run();
            }
        });
    }

    public void teleportWithRetry(Player player, String worldName) {
        ConfigManager.WorldConfig wc = configManager.getWorldConfig(worldName);
        if (wc == null) wc = configManager.getWorldConfig(configManager.getWorldDefault());
        if (wc == null) {
            if (!configManager.getWorldConfigs().isEmpty()) wc = configManager.getWorldConfigs().values().iterator().next();
            else {
                ChatUtil.send(player, configManager.getMessage("location", Map.of("attempts", "0")));
                return;
            }
        }

        final ConfigManager.WorldConfig finalWc = wc;
        World world = plugin.getServer().getWorld(finalWc.name);
        if (world == null) {
            ChatUtil.send(player, configManager.getMessage("world-error", Map.of("world", finalWc.name)));
            return;
        }

        ChatUtil.send(player, configManager.getMessage("location", Map.of("attempts", "1")).replace("cannot find", "Đang tìm..."));

        findSafeLocationAsync(finalWc.name).thenAccept(location -> {
            if (location == null) {
                plugin.getScheduler().runForEntity(plugin, player, () -> {
                    ChatUtil.send(player, configManager.getMessage("location", Map.of("attempts", String.valueOf(finalWc.maxAttempts))));
                    plugin.getQueueManager().handleTeleportFail(player);
                });
                return;
            }

            plugin.getScheduler().runAtLocation(plugin, location, () -> {
                Location safe = getHighestSafeY(world, location.getBlockX(), location.getBlockZ(), location.getBlockY(), finalWc);
                Location target = safe != null ? safe : location;

                teleportPlayer(player, target, () -> {
                    ChatUtil.send(player, configManager.getMessage("join", Map.of()));
                    if (configManager.isInvulnerabilityEnabled()) {
                        plugin.getQueueManager().giveInvulnerability(player);
                    }
                    if (configManager.isCooldownEnabled() && !player.hasPermission("rynixrtpq.bypass.cooldown") && !player.hasPermission("rtpqueue.bypass.cooldown") && !player.hasPermission("rtpq.bypass.cooldown")) {
                        plugin.getCooldownManager().setCooldown(player.getUniqueId(), configManager.getCooldownTime());
                    }
                    plugin.getSoundManager().playTeleportSound(player);

                }, () -> {
                    ChatUtil.send(player, configManager.getMessage("location", Map.of("attempts", String.valueOf(finalWc.maxAttempts))));
                    plugin.getQueueManager().handleTeleportFail(player);
                });
            });

        }).exceptionally(ex -> {
            plugin.getScheduler().runForEntity(plugin, player, () -> {
                ChatUtil.send(player, "&cLỗi RTP: " + ex.getMessage());
            });
            return null;
        });
    }

    public void teleportDuoWithRetry(List<Player> players, String worldName, Runnable onSuccess, Runnable onFail) {
        if (players == null || players.isEmpty()) {
            if (onFail != null) onFail.run();
            return;
        }

        ConfigManager.WorldConfig wc = configManager.getWorldConfig(worldName);
        if (wc == null) wc = configManager.getWorldConfig(configManager.getWorldDefault());
        if (wc == null) {
            if (!configManager.getWorldConfigs().isEmpty()) wc = configManager.getWorldConfigs().values().iterator().next();
            else {
                for (Player p : players) ChatUtil.send(p, configManager.getMessage("location", Map.of("attempts", "0")));
                if (onFail != null) onFail.run();
                return;
            }
        }

        final ConfigManager.WorldConfig finalWc = wc;
        World world = plugin.getServer().getWorld(finalWc.name);
        if (world == null) {
            for (Player p : players) ChatUtil.send(p, configManager.getMessage("world-error", Map.of("world", finalWc.name)));
            if (onFail != null) onFail.run();
            return;
        }

        for (Player p : players) {
            ChatUtil.send(p, "&aĐang tìm vị trí an toàn cho duo " + players.size() + " người...");
        }

        findSafeLocationAsync(finalWc.name).thenAccept(location -> {
            if (location == null) {
                for (Player p : players) {
                    plugin.getScheduler().runForEntity(plugin, p, () -> ChatUtil.send(p, configManager.getMessage("location", Map.of("attempts", String.valueOf(finalWc.maxAttempts)))));
                }
                if (onFail != null) plugin.getScheduler().run(plugin, onFail);
                return;
            }

            plugin.getScheduler().runAtLocation(plugin, location, () -> {
                Location safe = getHighestSafeY(world, location.getBlockX(), location.getBlockZ(), location.getBlockY(), finalWc);
                Location baseTarget = safe != null ? safe : location;

                int spread = configManager.getDuoSpreadDistance();
                ThreadLocalRandom random = ThreadLocalRandom.current();

                for (int i = 0; i < players.size(); i++) {
                    Player player = players.get(i);
                    Location target;
                    if (i == 0) {
                        target = baseTarget;
                    } else {
                        double offsetX = random.nextDouble(-spread, spread + 1);
                        double offsetZ = random.nextDouble(-spread, spread + 1);
                        target = baseTarget.clone().add(offsetX, 0, offsetZ);
                        Location safeSpread = getHighestSafeY(world, target.getBlockX(), target.getBlockZ(), target.getBlockY(), finalWc);
                        if (safeSpread != null) target = safeSpread;
                    }

                    final Location finalTarget = target;
                    teleportPlayer(player, finalTarget, () -> {
                        List<String> otherNames = players.stream().filter(pl -> !pl.getUniqueId().equals(player.getUniqueId())).map(Player::getName).toList();
                        String partnerStr = String.join(", ", otherNames);
                        ChatUtil.send(player, configManager.getMessage("duo-teleported", Map.of("partners", partnerStr, "world", finalWc.name)));
                        if (configManager.isInvulnerabilityEnabled()) plugin.getQueueManager().giveInvulnerability(player);
                        if (configManager.isCooldownEnabled() && !player.hasPermission("rynixrtpq.bypass.cooldown") && !player.hasPermission("rtpqueue.bypass.cooldown") && !player.hasPermission("rtpq.bypass.cooldown")) {
                            plugin.getCooldownManager().setCooldown(player.getUniqueId(), configManager.getCooldownTime());
                        }
                        plugin.getSoundManager().playTeleportSound(player);
                        plugin.getSoundManager().playSuccessSound(player);
                    }, () -> {
                        ChatUtil.send(player, configManager.getMessage("location", Map.of("attempts", String.valueOf(finalWc.maxAttempts))));
                    });
                }

                if (onSuccess != null) plugin.getScheduler().run(plugin, onSuccess);
            });

        }).exceptionally(ex -> {
            for (Player p : players) {
                plugin.getScheduler().runForEntity(plugin, p, () -> ChatUtil.send(p, "&cLỗi Duo RTP: " + ex.getMessage()));
            }
            if (onFail != null) plugin.getScheduler().run(plugin, onFail);
            return null;
        });
    }
}
