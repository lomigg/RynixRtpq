package com.rynix.rtpq.utils;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cooldown Manager - Thread-safe & Optimized
 * Plugin: RynixRtpq | Author: RinZz
 */
public class CooldownManager {

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public void setCooldown(UUID uuid, int seconds) {
        cooldowns.put(uuid, System.currentTimeMillis() + (seconds * 1000L));
    }

    public boolean hasCooldown(UUID uuid) {
        Long expire = cooldowns.get(uuid);
        if (expire == null) return false;
        if (System.currentTimeMillis() > expire) {
            cooldowns.remove(uuid);
            return false;
        }
        return true;
    }

    public long getRemaining(UUID uuid) {
        Long expire = cooldowns.get(uuid);
        if (expire == null) return 0;
        long rem = (expire - System.currentTimeMillis()) / 1000;
        return Math.max(0, rem);
    }

    public void removeCooldown(UUID uuid) {
        cooldowns.remove(uuid);
    }

    public void clear() {
        cooldowns.clear();
    }

    public void cleanup() {
        long now = System.currentTimeMillis();
        cooldowns.entrySet().removeIf(e -> e.getValue() < now);
    }
}
