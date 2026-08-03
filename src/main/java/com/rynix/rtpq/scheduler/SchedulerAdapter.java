package com.rynix.rtpq.scheduler;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Scheduler Adapter - Folia & Paper compatibility
 * Plugin: RynixRtpq | Author: RinZz
 */
public interface SchedulerAdapter {

    void run(Plugin plugin, Runnable task);
    void runAsync(Plugin plugin, Runnable task);
    void runDelayed(Plugin plugin, Runnable task, long delayTicks);
    void runAsyncDelayed(Plugin plugin, Runnable task, long delayTicks);
    void runAtLocation(Plugin plugin, Location location, Runnable task);
    void runDelayedAtLocation(Plugin plugin, Location location, Runnable task, long delayTicks);
    void runForEntity(Plugin plugin, Entity entity, Runnable task);
    void runDelayedForEntity(Plugin plugin, Entity entity, Runnable task, long delayTicks, Runnable retired);
    void runTimer(Plugin plugin, Runnable task, long delay, long period);
    void runAsyncTimer(Plugin plugin, Runnable task, long delay, long period);
    void cancelAll(Plugin plugin);
}
