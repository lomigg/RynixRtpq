package com.rynix.rtpq.queue;

import java.util.UUID;

/**
 * Queued Player Data - Lightweight & Optimized
 * Plugin: RynixRtpq | Author: RinZz
 */
public class QueuedPlayer {
    private final UUID uuid;
    private final String name;
    private final String worldName;
    private final long joinTime;
    private final boolean priority;
    private int attempts;

    public QueuedPlayer(UUID uuid, String name, String worldName, boolean priority) {
        this.uuid = uuid;
        this.name = name;
        this.worldName = worldName;
        this.joinTime = System.currentTimeMillis();
        this.priority = priority;
        this.attempts = 0;
    }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public String getWorldName() { return worldName; }
    public long getJoinTime() { return joinTime; }
    public boolean isPriority() { return priority; }
    public int getAttempts() { return attempts; }
    public void incrementAttempts() { attempts++; }

    public long getWaitTimeSeconds() {
        return (System.currentTimeMillis() - joinTime) / 1000;
    }
}
