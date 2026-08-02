package com.rynix.rtpq.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Folia Scheduler - Fully Regionized Multithreading Support
 * Plugin: RynixRtpq | Author: RinZz - Rynix Studio
 */
public class FoliaScheduler implements SchedulerAdapter {

    @Override
    public void run(Plugin plugin, Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    @Override
    public void runAsync(Plugin plugin, Runnable task) {
        Bukkit.getAsyncScheduler().runNow(plugin, t -> task.run());
    }

    @Override
    public void runDelayed(Plugin plugin, Runnable task, long delayTicks) {
        Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> task.run(), delayTicks);
    }

    @Override
    public void runAsyncDelayed(Plugin plugin, Runnable task, long delayTicks) {
        long delayMs = delayTicks * 50L;
        Bukkit.getAsyncScheduler().runDelayed(plugin, t -> task.run(), delayMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void runAtLocation(Plugin plugin, Location location, Runnable task) {
        if (location == null || location.getWorld() == null) {
            run(plugin, task);
            return;
        }
        Bukkit.getRegionScheduler().execute(plugin, location, task);
    }

    @Override
    public void runDelayedAtLocation(Plugin plugin, Location location, Runnable task, long delayTicks) {
        if (location == null || location.getWorld() == null) {
            runDelayed(plugin, task, delayTicks);
            return;
        }
        Bukkit.getRegionScheduler().runDelayed(plugin, location, t -> task.run(), delayTicks);
    }

    @Override
    public void runForEntity(Plugin plugin, Entity entity, Runnable task) {
        if (entity == null) {
            run(plugin, task);
            return;
        }
        entity.getScheduler().execute(plugin, task, null, 1L);
    }

    @Override
    public void runDelayedForEntity(Plugin plugin, Entity entity, Runnable task, long delayTicks, Runnable retired) {
        if (entity == null) {
            runDelayed(plugin, task, delayTicks);
            return;
        }
        entity.getScheduler().runDelayed(plugin, t -> task.run(), retired, delayTicks);
    }

    @Override
    public void runTimer(Plugin plugin, Runnable task, long delay, long period) {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, t -> task.run(), delay, period);
    }

    @Override
    public void runAsyncTimer(Plugin plugin, Runnable task, long delay, long period) {
        long delayMs = delay * 50L;
        long periodMs = period * 50L;
        Bukkit.getAsyncScheduler().runAtFixedRate(plugin, t -> task.run(), delayMs, periodMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public void cancelAll(Plugin plugin) {
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
        try {
            Bukkit.getRegionScheduler().cancelTasks(plugin);
        } catch (Exception ignored) {}
    }
}
